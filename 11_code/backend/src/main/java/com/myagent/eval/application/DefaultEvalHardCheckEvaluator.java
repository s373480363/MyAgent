package com.myagent.eval.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 默认 hardChecks 执行器。
 */
@Component
class DefaultEvalHardCheckEvaluator implements EvalHardCheckEvaluator {

    /**
     * JSON 对象映射器。
     */
    private final ObjectMapper objectMapper;

    /**
     * 构造 hardChecks 执行器。
     *
     * @param objectMapper JSON 对象映射器
     */
    DefaultEvalHardCheckEvaluator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 逐条执行 hardChecks，并汇总首个失败原因。
     *
     * @param output 节点输出
     * @param hardChecks hardChecks 配置数组
     * @param schemaValidationResultJson Schema 校验结果
     * @return hardChecks 执行结果
     */
    @Override
    public EvalHardCheckEvaluation evaluate(JsonNode output, JsonNode hardChecks, JsonNode schemaValidationResultJson) {
        ArrayNode resultArray = objectMapper.createArrayNode();
        if (hardChecks == null || hardChecks.isNull() || hardChecks.isMissingNode()) {
            return new EvalHardCheckEvaluation(true, resultArray, "");
        }
        if (!hardChecks.isArray()) {
            resultArray.add(invalidHardChecksResult("hardChecks 配置必须是数组。"));
            return new EvalHardCheckEvaluation(false, resultArray, "hardChecks 配置必须是数组。");
        }

        boolean passed = true;
        String firstFailure = "";
        // V1 hardChecks 结果顺序必须与输入配置保持一致，便于前端和排障直接对位。
        for (JsonNode hardCheck : hardChecks) {
            ObjectNode result = evaluateOne(output, hardCheck, schemaValidationResultJson);
            resultArray.add(result);
            if (!result.path("passed").asBoolean(false)) {
                passed = false;
                if (firstFailure.isBlank()) {
                    firstFailure = result.path("message").asText("hardChecks 未通过。");
                }
            }
        }
        return new EvalHardCheckEvaluation(passed, resultArray, firstFailure);
    }

    /**
     * 执行单条 hardCheck。
     *
     * @param output 节点输出
     * @param hardCheck 单条 hardCheck 配置
     * @param schemaValidationResultJson Schema 校验结果
     * @return 单条 hardCheck 执行结果
     */
    private ObjectNode evaluateOne(JsonNode output, JsonNode hardCheck, JsonNode schemaValidationResultJson) {
        if (hardCheck == null || !hardCheck.isObject()) {
            return invalidHardChecksResult("hardChecks 配置项必须是对象。");
        }
        String type = hardCheck.path("type").asText("");
        String path = hardCheck.path("path").asText(null);
        // V1 只允许执行已冻结的 7 种 hardCheck 类型，禁止兼容旧 assertions 语义。
        return switch (type) {
            case "SCHEMA_VALIDATION" -> schemaValidationResult(schemaValidationResultJson);
            case "JSON_PATH_EXISTS" -> existsResult(path, read(output, path));
            case "JSON_PATH_IN" -> inValuesResult(path, read(output, path), hardCheck.get("values"));
            case "JSON_PATH_NUMBER_RANGE" -> numberRangeResult(path, read(output, path), hardCheck);
            case "JSON_PATH_REGEX" -> regexResult(path, read(output, path), hardCheck.path("pattern").asText(""));
            case "JSON_PATH_CONTAINS" -> containsResult(path, read(output, path), hardCheck.path("expected").asText(""), true);
            case "JSON_PATH_NOT_CONTAINS" -> containsResult(path, read(output, path), hardCheck.path("expected").asText(""), false);
            default -> invalidTypeResult(type);
        };
    }

    /**
     * 构造 hardChecks 配置非法时的统一结果。
     *
     * @param message 错误消息
     * @return 统一结果节点
     */
    private ObjectNode invalidHardChecksResult(String message) {
        return result(
                "INVALID_HARD_CHECKS",
                false,
                message,
                null,
                textNode("数组"),
                null,
                objectMapper.createObjectNode()
        );
    }

    /**
     * 构造不支持类型时的统一结果。
     *
     * @param type 当前类型
     * @return 统一结果节点
     */
    private ObjectNode invalidTypeResult(String type) {
        return result(
                type == null || type.isBlank() ? "UNKNOWN" : type,
                false,
                "不支持的 hardCheck 类型：" + type,
                null,
                null,
                null,
                objectMapper.createObjectNode()
        );
    }

    /**
     * 读取节点输出的 Schema 校验结果，并转换为 hardCheck 结果。
     *
     * @param schemaValidationResultJson Schema 校验结果
     * @return Schema 校验对应的 hardCheck 结果
     */
    private ObjectNode schemaValidationResult(JsonNode schemaValidationResultJson) {
        if (schemaValidationResultJson == null
                || schemaValidationResultJson.isNull()
                || !schemaValidationResultJson.isObject()) {
            return result(
                    "SCHEMA_VALIDATION",
                    false,
                    "未执行节点输出 Schema 校验。",
                    null,
                    textNode("节点输出通过 Schema 校验"),
                    null,
                    objectMapper.createObjectNode()
            );
        }

        JsonNode results = schemaValidationResultJson.path("results");
        if (!results.isArray()) {
            return result(
                    "SCHEMA_VALIDATION",
                    false,
                    "未执行节点输出 Schema 校验。",
                    null,
                    textNode("节点输出通过 Schema 校验"),
                    null,
                    objectMapper.createObjectNode()
            );
        }

        // 只读取节点输出阶段的校验结果，其他阶段不参与 V1 hardChecks 判定。
        for (JsonNode schemaResult : results) {
            if ("NODE_OUTPUT".equals(schemaResult.path("stage").asText())) {
                boolean passed = schemaResult.path("valid").asBoolean(false);
                return result(
                        "SCHEMA_VALIDATION",
                        passed,
                        passed
                                ? "节点输出 Schema 校验通过。"
                                : "节点输出 Schema 校验未通过。",
                        null,
                        textNode("节点输出通过 Schema 校验"),
                        boolNode(passed),
                        schemaResult
                );
            }
        }

        return result(
                "SCHEMA_VALIDATION",
                false,
                "未执行节点输出 Schema 校验。",
                null,
                textNode("节点输出通过 Schema 校验"),
                null,
                objectMapper.createObjectNode()
        );
    }

    /**
     * 执行 JSON_PATH_EXISTS 检查。
     *
     * @param path JSONPath
     * @param actual 实际值
     * @return 执行结果
     */
    private ObjectNode existsResult(String path, JsonNode actual) {
        boolean passed = actual != null && !(actual instanceof MissingNode);
        return result(
                "JSON_PATH_EXISTS",
                passed,
                passed ? path + " 字段存在。" : path + " 字段缺失。",
                path,
                textNode("字段存在"),
                actualNode(actual),
                objectMapper.createObjectNode()
        );
    }

    /**
     * 执行 JSON_PATH_IN 检查。
     *
     * @param path JSONPath
     * @param actual 实际值
     * @param values 允许值数组
     * @return 执行结果
     */
    private ObjectNode inValuesResult(String path, JsonNode actual, JsonNode values) {
        if (values == null || !values.isArray() || values.isEmpty()) {
            return result(
                    "JSON_PATH_IN",
                    false,
                    "hardCheck values 配置不合法。",
                    path,
                    values == null ? objectMapper.nullNode() : values,
                    actualNode(actual),
                    objectMapper.createObjectNode()
            );
        }
        for (JsonNode value : values) {
            if (value.equals(actual)) {
                return result(
                        "JSON_PATH_IN",
                        true,
                        "枚举检查通过。",
                        path,
                        values,
                        actualNode(actual),
                        objectMapper.createObjectNode()
                );
            }
        }
        return result(
                "JSON_PATH_IN",
                false,
                path + " 不在允许枚举值内。",
                path,
                values,
                actualNode(actual),
                objectMapper.createObjectNode()
        );
    }

    /**
     * 执行 JSON_PATH_NUMBER_RANGE 检查。
     *
     * @param path JSONPath
     * @param actual 实际值
     * @param hardCheck hardCheck 配置
     * @return 执行结果
     */
    private ObjectNode numberRangeResult(String path, JsonNode actual, JsonNode hardCheck) {
        ObjectNode expected = objectMapper.createObjectNode();
        JsonNode minNode = hardCheck.get("min");
        JsonNode maxNode = hardCheck.get("max");
        if (minNode != null) {
            expected.set("min", minNode);
        }
        if (maxNode != null) {
            expected.set("max", maxNode);
        }
        if (actual == null || !actual.isNumber()) {
            return result(
                    "JSON_PATH_NUMBER_RANGE",
                    false,
                    path + " 不是数值。",
                    path,
                    expected,
                    actualNode(actual),
                    objectMapper.createObjectNode()
            );
        }
        BigDecimal actualValue = actual.decimalValue();
        boolean minOk = minNode == null || !minNode.isNumber() || actualValue.compareTo(minNode.decimalValue()) >= 0;
        boolean maxOk = maxNode == null || !maxNode.isNumber() || actualValue.compareTo(maxNode.decimalValue()) <= 0;
        boolean passed = minOk && maxOk;
        return result(
                "JSON_PATH_NUMBER_RANGE",
                passed,
                passed ? "数值范围检查通过。" : path + " 不在允许范围内。",
                path,
                expected,
                actual,
                objectMapper.createObjectNode()
        );
    }

    /**
     * 执行 JSON_PATH_REGEX 检查。
     *
     * @param path JSONPath
     * @param actual 实际值
     * @param pattern 正则表达式
     * @return 执行结果
     */
    private ObjectNode regexResult(String path, JsonNode actual, String pattern) {
        if (actual == null || !actual.isTextual()) {
            return result(
                    "JSON_PATH_REGEX",
                    false,
                    path + " 不是字符串。",
                    path,
                    textNode(pattern),
                    actualNode(actual),
                    objectMapper.createObjectNode()
            );
        }
        boolean passed = Pattern.compile(pattern).matcher(actual.asText()).find();
        return result(
                "JSON_PATH_REGEX",
                passed,
                passed ? "正则检查通过。" : path + " 未匹配正则表达式。",
                path,
                textNode(pattern),
                actual,
                objectMapper.createObjectNode()
        );
    }

    /**
     * 执行 JSON_PATH_CONTAINS 或 JSON_PATH_NOT_CONTAINS 检查。
     *
     * @param path JSONPath
     * @param actual 实际值
     * @param expected 目标文本
     * @param shouldContain 是否要求包含
     * @return 执行结果
     */
    private ObjectNode containsResult(String path, JsonNode actual, String expected, boolean shouldContain) {
        if (actual == null || !actual.isTextual()) {
            return result(
                    shouldContain ? "JSON_PATH_CONTAINS" : "JSON_PATH_NOT_CONTAINS",
                    false,
                    path + " 不是字符串。",
                    path,
                    textNode(expected),
                    actualNode(actual),
                    objectMapper.createObjectNode()
            );
        }
        boolean contains = actual.asText().contains(expected);
        boolean passed = shouldContain == contains;
        String message;
        if (passed) {
            message = shouldContain ? "包含检查通过。" : "排除检查通过。";
        } else {
            message = shouldContain ? path + " 未包含期望文本。" : path + " 包含了禁止文本。";
        }
        return result(
                shouldContain ? "JSON_PATH_CONTAINS" : "JSON_PATH_NOT_CONTAINS",
                passed,
                message,
                path,
                textNode(expected),
                actual,
                objectMapper.createObjectNode()
        );
    }

    /**
     * 构造统一 hardCheck 结果节点。
     *
     * @param type hardCheck 类型
     * @param passed 是否通过
     * @param message 结果摘要
     * @param path JSONPath
     * @param expected 期望值
     * @param actual 实际值
     * @param details 额外细节
     * @return 结果节点
     */
    private ObjectNode result(
            String type,
            boolean passed,
            String message,
            String path,
            JsonNode expected,
            JsonNode actual,
            JsonNode details
    ) {
        ObjectNode node = objectMapper.createObjectNode()
                .put("type", type)
                .put("passed", passed)
                .put("message", message);
        if (path != null && !path.isBlank()) {
            node.put("path", path);
        }
        if (expected != null) {
            node.set("expected", expected);
        }
        if (actual != null) {
            node.set("actual", actual);
        }
        if (details != null) {
            node.set("details", details);
        }
        return node;
    }

    /**
     * 按受控 JSONPath 读取节点输出值。
     *
     * @param root 根节点
     * @param path JSONPath
     * @return 读取到的值；找不到时返回 MissingNode
     */
    private JsonNode read(JsonNode root, String path) {
        if (root == null || path == null || path.isBlank() || path.charAt(0) != '$') {
            return MissingNode.getInstance();
        }
        JsonNode current = root;
        // 这里只支持 $.field 和 [index] 的受控子集，避免引入完整 JSONPath 解释器复杂度。
        for (PathToken token : parse(path)) {
            if (token.fieldName() != null) {
                if (current == null || !current.isObject() || !current.has(token.fieldName())) {
                    return MissingNode.getInstance();
                }
                current = current.get(token.fieldName());
            } else {
                if (current == null || !current.isArray() || token.arrayIndex() >= current.size()) {
                    return MissingNode.getInstance();
                }
                current = current.get(token.arrayIndex());
            }
        }
        return current == null ? MissingNode.getInstance() : current;
    }

    /**
     * 解析受控 JSONPath。
     *
     * @param path JSONPath
     * @return 解析后的路径令牌列表
     */
    private List<PathToken> parse(String path) {
        List<PathToken> tokens = new ArrayList<>();
        int index = 1;
        // 一旦路径格式不满足 V1 支持子集，立即返回非法占位令牌，让读取逻辑统一失败。
        while (index < path.length()) {
            char current = path.charAt(index);
            if (current == '.') {
                int start = ++index;
                while (index < path.length() && path.charAt(index) != '.' && path.charAt(index) != '[') {
                    index++;
                }
                if (start == index) {
                    return List.of(new PathToken("__invalid__", null));
                }
                tokens.add(new PathToken(path.substring(start, index), null));
                continue;
            }
            if (current == '[') {
                int end = path.indexOf(']', index);
                if (end < 0) {
                    return List.of(new PathToken("__invalid__", null));
                }
                int arrayIndex;
                try {
                    arrayIndex = Integer.parseInt(path.substring(index + 1, end));
                } catch (NumberFormatException exception) {
                    return List.of(new PathToken("__invalid__", null));
                }
                tokens.add(new PathToken(null, arrayIndex));
                index = end + 1;
                continue;
            }
            return List.of(new PathToken("__invalid__", null));
        }
        return tokens;
    }

    /**
     * 将读取结果规范化为可回写的 actual 节点。
     *
     * @param actual 原始读取结果
     * @return 规范化后的 actual 节点
     */
    private JsonNode actualNode(JsonNode actual) {
        if (actual == null || actual.isMissingNode()) {
            return objectMapper.nullNode();
        }
        return actual;
    }

    /**
     * 构造文本节点。
     *
     * @param text 文本值
     * @return 文本节点
     */
    private JsonNode textNode(String text) {
        return JsonNodeFactory.instance.textNode(text);
    }

    /**
     * 构造布尔节点。
     *
     * @param value 布尔值
     * @return 布尔节点
     */
    private JsonNode boolNode(boolean value) {
        return JsonNodeFactory.instance.booleanNode(value);
    }

    /**
     * 受控 JSONPath 解析后的单个路径令牌。
     *
     * @param fieldName 对象字段名
     * @param arrayIndex 数组下标
     */
    private record PathToken(String fieldName, Integer arrayIndex) {
    }
}
