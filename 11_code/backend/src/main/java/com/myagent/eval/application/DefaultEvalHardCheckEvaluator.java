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
 * Hard check evaluator.
 */
@Component
class DefaultEvalHardCheckEvaluator implements EvalHardCheckEvaluator {

    private final ObjectMapper objectMapper;

    DefaultEvalHardCheckEvaluator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public EvalHardCheckEvaluation evaluate(JsonNode output, JsonNode hardChecks, JsonNode schemaValidationResultJson) {
        ArrayNode resultArray = objectMapper.createArrayNode();
        if (hardChecks == null || hardChecks.isNull() || hardChecks.isMissingNode()) {
            return new EvalHardCheckEvaluation(true, resultArray, "");
        }
        if (!hardChecks.isArray()) {
            resultArray.add(invalidHardChecksResult("hardChecks \u914d\u7f6e\u5fc5\u987b\u662f\u6570\u7ec4\u3002"));
            return new EvalHardCheckEvaluation(false, resultArray, "hardChecks \u914d\u7f6e\u5fc5\u987b\u662f\u6570\u7ec4\u3002");
        }

        boolean passed = true;
        String firstFailure = "";
        for (JsonNode hardCheck : hardChecks) {
            ObjectNode result = evaluateOne(output, hardCheck, schemaValidationResultJson);
            resultArray.add(result);
            if (!result.path("passed").asBoolean(false)) {
                passed = false;
                if (firstFailure.isBlank()) {
                    firstFailure = result.path("message").asText("hardChecks \u672a\u901a\u8fc7\u3002");
                }
            }
        }
        return new EvalHardCheckEvaluation(passed, resultArray, firstFailure);
    }

    private ObjectNode evaluateOne(JsonNode output, JsonNode hardCheck, JsonNode schemaValidationResultJson) {
        if (hardCheck == null || !hardCheck.isObject()) {
            return invalidHardChecksResult("hardChecks \u914d\u7f6e\u9879\u5fc5\u987b\u662f\u5bf9\u8c61\u3002");
        }
        String type = hardCheck.path("type").asText("");
        String path = hardCheck.path("path").asText(null);
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

    private ObjectNode invalidHardChecksResult(String message) {
        return result(
                "INVALID_HARD_CHECKS",
                false,
                message,
                null,
                textNode("\u6570\u7ec4"),
                null,
                objectMapper.createObjectNode()
        );
    }

    private ObjectNode invalidTypeResult(String type) {
        return result(
                type == null || type.isBlank() ? "UNKNOWN" : type,
                false,
                "\u4e0d\u652f\u6301\u7684 hardCheck \u7c7b\u578b\uff1a" + type,
                null,
                null,
                null,
                objectMapper.createObjectNode()
        );
    }

    private ObjectNode schemaValidationResult(JsonNode schemaValidationResultJson) {
        if (schemaValidationResultJson == null
                || schemaValidationResultJson.isNull()
                || !schemaValidationResultJson.isObject()) {
            return result(
                    "SCHEMA_VALIDATION",
                    false,
                    "\u672a\u6267\u884c\u8282\u70b9\u8f93\u51fa Schema \u6821\u9a8c\u3002",
                    null,
                    textNode("\u8282\u70b9\u8f93\u51fa\u901a\u8fc7 Schema \u6821\u9a8c"),
                    null,
                    objectMapper.createObjectNode()
            );
        }

        JsonNode results = schemaValidationResultJson.path("results");
        if (!results.isArray()) {
            return result(
                    "SCHEMA_VALIDATION",
                    false,
                    "\u672a\u6267\u884c\u8282\u70b9\u8f93\u51fa Schema \u6821\u9a8c\u3002",
                    null,
                    textNode("\u8282\u70b9\u8f93\u51fa\u901a\u8fc7 Schema \u6821\u9a8c"),
                    null,
                    objectMapper.createObjectNode()
            );
        }

        for (JsonNode schemaResult : results) {
            if ("NODE_OUTPUT".equals(schemaResult.path("stage").asText())) {
                boolean passed = schemaResult.path("valid").asBoolean(false);
                return result(
                        "SCHEMA_VALIDATION",
                        passed,
                        passed
                                ? "\u8282\u70b9\u8f93\u51fa Schema \u6821\u9a8c\u901a\u8fc7\u3002"
                                : "\u8282\u70b9\u8f93\u51fa Schema \u6821\u9a8c\u672a\u901a\u8fc7\u3002",
                        null,
                        textNode("\u8282\u70b9\u8f93\u51fa\u901a\u8fc7 Schema \u6821\u9a8c"),
                        boolNode(passed),
                        schemaResult
                );
            }
        }

        return result(
                "SCHEMA_VALIDATION",
                false,
                "\u672a\u6267\u884c\u8282\u70b9\u8f93\u51fa Schema \u6821\u9a8c\u3002",
                null,
                textNode("\u8282\u70b9\u8f93\u51fa\u901a\u8fc7 Schema \u6821\u9a8c"),
                null,
                objectMapper.createObjectNode()
        );
    }

    private ObjectNode existsResult(String path, JsonNode actual) {
        boolean passed = actual != null && !(actual instanceof MissingNode);
        return result(
                "JSON_PATH_EXISTS",
                passed,
                passed ? path + " \u5b57\u6bb5\u5b58\u5728\u3002" : path + " \u5b57\u6bb5\u7f3a\u5931\u3002",
                path,
                textNode("\u5b57\u6bb5\u5b58\u5728"),
                actualNode(actual),
                objectMapper.createObjectNode()
        );
    }

    private ObjectNode inValuesResult(String path, JsonNode actual, JsonNode values) {
        if (values == null || !values.isArray() || values.isEmpty()) {
            return result(
                    "JSON_PATH_IN",
                    false,
                    "hardCheck values \u914d\u7f6e\u4e0d\u5408\u6cd5\u3002",
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
                        "\u679a\u4e3e\u68c0\u67e5\u901a\u8fc7\u3002",
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
                path + " \u4e0d\u5728\u5141\u8bb8\u679a\u4e3e\u503c\u5185\u3002",
                path,
                values,
                actualNode(actual),
                objectMapper.createObjectNode()
        );
    }

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
                    path + " \u4e0d\u662f\u6570\u503c\u3002",
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
                passed ? "\u6570\u503c\u8303\u56f4\u68c0\u67e5\u901a\u8fc7\u3002" : path + " \u4e0d\u5728\u5141\u8bb8\u8303\u56f4\u5185\u3002",
                path,
                expected,
                actual,
                objectMapper.createObjectNode()
        );
    }

    private ObjectNode regexResult(String path, JsonNode actual, String pattern) {
        if (actual == null || !actual.isTextual()) {
            return result(
                    "JSON_PATH_REGEX",
                    false,
                    path + " \u4e0d\u662f\u5b57\u7b26\u4e32\u3002",
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
                passed ? "\u6b63\u5219\u68c0\u67e5\u901a\u8fc7\u3002" : path + " \u672a\u5339\u914d\u6b63\u5219\u8868\u8fbe\u5f0f\u3002",
                path,
                textNode(pattern),
                actual,
                objectMapper.createObjectNode()
        );
    }

    private ObjectNode containsResult(String path, JsonNode actual, String expected, boolean shouldContain) {
        if (actual == null || !actual.isTextual()) {
            return result(
                    shouldContain ? "JSON_PATH_CONTAINS" : "JSON_PATH_NOT_CONTAINS",
                    false,
                    path + " \u4e0d\u662f\u5b57\u7b26\u4e32\u3002",
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
            message = shouldContain ? "\u5305\u542b\u68c0\u67e5\u901a\u8fc7\u3002" : "\u6392\u9664\u68c0\u67e5\u901a\u8fc7\u3002";
        } else {
            message = shouldContain ? path + " \u672a\u5305\u542b\u671f\u671b\u6587\u672c\u3002" : path + " \u5305\u542b\u4e86\u7981\u6b62\u6587\u672c\u3002";
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

    private JsonNode actualNode(JsonNode actual) {
        if (actual == null || actual.isMissingNode()) {
            return objectMapper.nullNode();
        }
        return actual;
    }

    private JsonNode textNode(String text) {
        return JsonNodeFactory.instance.textNode(text);
    }

    private JsonNode boolNode(boolean value) {
        return JsonNodeFactory.instance.booleanNode(value);
    }

    private record PathToken(String fieldName, Integer arrayIndex) {
    }
}
