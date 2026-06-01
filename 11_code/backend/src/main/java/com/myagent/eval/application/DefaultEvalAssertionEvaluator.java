package com.myagent.eval.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 默认验收断言执行器。
 */
@Component
class DefaultEvalAssertionEvaluator implements EvalAssertionEvaluator {

    /**
     * JSON 对象映射器。
     */
    private final ObjectMapper objectMapper;

    /**
     * 构造断言执行器。
     *
     * @param objectMapper JSON 对象映射器
     */
    DefaultEvalAssertionEvaluator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 执行确定性断言。
     *
     * @param output 节点输出
     * @param assertions 断言定义
     * @param schemaValidationResultJson 节点 Schema 校验结果
     * @return 断言结果
     */
    @Override
    public EvalAssertionEvaluation evaluate(JsonNode output, JsonNode assertions, JsonNode schemaValidationResultJson) {
        ArrayNode resultArray = objectMapper.createArrayNode();
        if (assertions == null || assertions.isNull() || assertions.isMissingNode()) {
            ObjectNode result = resultArray.addObject();
            result.put("type", "INVALID_ASSERTIONS");
            result.put("passed", false);
            result.put("message", "断言配置必须是非空数组。");
            return new EvalAssertionEvaluation(false, resultArray, "断言配置必须是非空数组。");
        }
        if (!assertions.isArray()) {
            ObjectNode result = resultArray.addObject();
            result.put("type", "INVALID_ASSERTIONS");
            result.put("passed", false);
            result.put("message", "断言配置必须是数组。");
            return new EvalAssertionEvaluation(false, resultArray, "断言配置必须是数组。");
        }
        if (assertions.isEmpty()) {
            ObjectNode result = resultArray.addObject();
            result.put("type", "INVALID_ASSERTIONS");
            result.put("passed", false);
            result.put("message", "断言配置不能为空。");
            return new EvalAssertionEvaluation(false, resultArray, "断言配置不能为空。");
        }
        boolean passed = true;
        String firstFailure = "";
        for (JsonNode assertion : assertions) {
            AssertionResult assertionResult = evaluateOne(output, assertion, schemaValidationResultJson);
            ObjectNode result = resultArray.addObject();
            result.put("type", assertionResult.type());
            result.put("passed", assertionResult.passed());
            result.put("message", assertionResult.message());
            if (!assertionResult.passed()) {
                passed = false;
                if (firstFailure.isBlank()) {
                    firstFailure = assertionResult.message();
                }
            }
        }
        return new EvalAssertionEvaluation(passed, resultArray, firstFailure);
    }

    /**
     * 执行单条断言。
     *
     * @param output 输出 JSON
     * @param assertion 断言定义
     * @param schemaValidationResultJson 节点 Schema 校验结果
     * @return 断言结果
     */
    private AssertionResult evaluateOne(JsonNode output, JsonNode assertion, JsonNode schemaValidationResultJson) {
        String type = text(assertion, "type", "");
        String path = text(assertion, "path", "$");
        JsonNode actual = read(output, path);
        return switch (type) {
            case "JSON_PATH_EXISTS" -> exists(type, path, actual);
            case "JSON_PATH_EQUALS", "FIELD_EQUALS" -> equals(type, path, actual, expected(assertion));
            case "JSON_PATH_CONTAINS", "CONTAINS" -> contains(type, path, actual, expected(assertion), true);
            case "JSON_PATH_NOT_CONTAINS", "NOT_CONTAINS" -> contains(type, path, actual, expected(assertion), false);
            case "JSON_PATH_REGEX", "REGEX_MATCH" -> regex(type, path, actual, text(assertion, "pattern", ""));
            case "JSON_PATH_NUMBER_RANGE", "NUMERIC_RANGE" -> range(type, path, actual, assertion);
            case "JSON_PATH_IN", "ENUM" -> inValues(type, path, actual, assertion.get("values"));
            case "SCHEMA_VALIDATION" -> schemaValidation(type, schemaValidationResultJson);
            default -> new AssertionResult(type.isBlank() ? "UNKNOWN" : type, false, "不支持的断言类型：" + type);
        };
    }

    /**
     * 判断节点输出 Schema 校验是否真实发生且通过。
     *
     * @param type 断言类型
     * @param schemaValidationResultJson 节点 Schema 校验结果
     * @return 断言结果
     */
    private AssertionResult schemaValidation(String type, JsonNode schemaValidationResultJson) {
        if (schemaValidationResultJson == null
                || schemaValidationResultJson.isNull()
                || !schemaValidationResultJson.isObject()) {
            return new AssertionResult(type, false, "未执行节点输出 Schema 校验。");
        }
        JsonNode results = schemaValidationResultJson.path("results");
        if (!results.isArray()) {
            return new AssertionResult(type, false, "未执行节点输出 Schema 校验。");
        }
        for (JsonNode result : results) {
            if ("NODE_OUTPUT".equals(result.path("stage").asText())) {
                boolean passed = result.path("valid").asBoolean(false);
                return new AssertionResult(
                        type,
                        passed,
                        passed ? "节点输出 Schema 校验已通过。" : "节点输出 Schema 校验未通过。"
                );
            }
        }
        return new AssertionResult(type, false, "未执行节点输出 Schema 校验。");
    }

    /**
     * 判断路径存在。
     *
     * @param type 断言类型
     * @param path 路径
     * @param actual 实际值
     * @return 断言结果
     */
    private AssertionResult exists(String type, String path, JsonNode actual) {
        boolean passed = actual != null && !(actual instanceof MissingNode);
        return new AssertionResult(type, passed, passed ? path + " 存在。" : path + " 字段缺失。");
    }

    /**
     * 判断值相等。
     *
     * @param type 断言类型
     * @param path 路径
     * @param actual 实际值
     * @param expected 期望值
     * @return 断言结果
     */
    private AssertionResult equals(String type, String path, JsonNode actual, JsonNode expected) {
        boolean passed = actual != null && !(actual instanceof MissingNode) && actual.equals(expected);
        return new AssertionResult(type, passed, passed ? path + " 等于期望值。" : path + " 不等于期望值。");
    }

    /**
     * 判断包含或不包含。
     *
     * @param type 断言类型
     * @param path 路径
     * @param actual 实际值
     * @param expected 期望值
     * @param shouldContain 是否应包含
     * @return 断言结果
     */
    private AssertionResult contains(String type, String path, JsonNode actual, JsonNode expected, boolean shouldContain) {
        boolean contains = false;
        if (actual != null && actual.isTextual() && expected != null) {
            contains = actual.asText().contains(expected.asText());
        } else if (actual != null && actual.isArray() && expected != null) {
            for (JsonNode item : actual) {
                if (item.equals(expected)) {
                    contains = true;
                    break;
                }
            }
        }
        boolean passed = shouldContain == contains;
        String message = shouldContain ? path + " 未包含期望内容。" : path + " 包含了禁止内容。";
        return new AssertionResult(type, passed, passed ? "包含关系断言通过。" : message);
    }

    /**
     * 正则匹配。
     *
     * @param type 断言类型
     * @param path 路径
     * @param actual 实际值
     * @param pattern 正则表达式
     * @return 断言结果
     */
    private AssertionResult regex(String type, String path, JsonNode actual, String pattern) {
        boolean passed = actual != null && actual.isTextual() && Pattern.compile(pattern).matcher(actual.asText()).find();
        return new AssertionResult(type, passed, passed ? "正则断言通过。" : path + " 未匹配正则表达式。");
    }

    /**
     * 数值范围判断。
     *
     * @param type 断言类型
     * @param path 路径
     * @param actual 实际值
     * @param assertion 断言定义
     * @return 断言结果
     */
    private AssertionResult range(String type, String path, JsonNode actual, JsonNode assertion) {
        if (actual == null || !actual.isNumber()) {
            return new AssertionResult(type, false, path + " 不是数值。");
        }
        BigDecimal value = actual.decimalValue();
        JsonNode minNode = assertion.get("min");
        JsonNode maxNode = assertion.get("max");
        boolean minOk = minNode == null || !minNode.isNumber() || value.compareTo(minNode.decimalValue()) >= 0;
        boolean maxOk = maxNode == null || !maxNode.isNumber() || value.compareTo(maxNode.decimalValue()) <= 0;
        boolean passed = minOk && maxOk;
        return new AssertionResult(type, passed, passed ? "数值范围断言通过。" : path + " 不在允许范围内。");
    }

    /**
     * 枚举判断。
     *
     * @param type 断言类型
     * @param path 路径
     * @param actual 实际值
     * @param values 允许值
     * @return 断言结果
     */
    private AssertionResult inValues(String type, String path, JsonNode actual, JsonNode values) {
        if (values == null || !values.isArray()) {
            return new AssertionResult(type, false, "枚举断言缺少 values 数组。");
        }
        for (JsonNode value : values) {
            if (value.equals(actual)) {
                return new AssertionResult(type, true, "枚举断言通过。");
            }
        }
        return new AssertionResult(type, false, path + " 不在允许枚举值内。");
    }

    /**
     * 读取期望值。
     *
     * @param assertion 断言定义
     * @return 期望值
     */
    private JsonNode expected(JsonNode assertion) {
        if (assertion == null) {
            return objectMapper.nullNode();
        }
        JsonNode expected = assertion.get("expected");
        if (expected == null) {
            expected = assertion.get("value");
        }
        return expected == null ? objectMapper.nullNode() : expected;
    }

    /**
     * 读取文本字段。
     *
     * @param node JSON 节点
     * @param fieldName 字段名
     * @param fallback 回退值
     * @return 文本值
     */
    private String text(JsonNode node, String fieldName, String fallback) {
        if (node != null && node.hasNonNull(fieldName) && node.get(fieldName).isTextual()) {
            return node.get(fieldName).asText();
        }
        return fallback;
    }

    /**
     * 按受控 JSONPath 读取值。
     *
     * @param root 根节点
     * @param path 路径
     * @return 读取到的节点或 MissingNode
     */
    private JsonNode read(JsonNode root, String path) {
        if (root == null || path == null || path.isBlank() || path.charAt(0) != '$') {
            return MissingNode.getInstance();
        }
        JsonNode current = root;
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
     * @param path 路径
     * @return 路径片段
     */
    private List<PathToken> parse(String path) {
        List<PathToken> tokens = new ArrayList<>();
        int index = 1;
        while (index < path.length()) {
            char current = path.charAt(index);
            if (current == '.') {
                int start = ++index;
                while (index < path.length() && path.charAt(index) != '.' && path.charAt(index) != '[') {
                    index++;
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
     * 单条断言结果。
     *
     * @param type 类型
     * @param passed 是否通过
     * @param message 消息
     */
    private record AssertionResult(String type, boolean passed, String message) {
    }

    /**
     * JSONPath 片段。
     *
     * @param fieldName 字段名
     * @param arrayIndex 数组下标
     */
    private record PathToken(String fieldName, Integer arrayIndex) {
    }
}
