package com.myagent.eval.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.myagent.common.error.BizException;
import com.myagent.common.error.ErrorCode;
import com.myagent.eval.repository.EvalCaseRecord;
import com.myagent.workflow.domain.WorkflowNodeDefinition;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Formal eval case validation service.
 */
@Component
public class EvalCaseFormalValidationService {

    private static final Set<String> SUPPORTED_HARD_CHECK_TYPES = Set.of(
            "SCHEMA_VALIDATION",
            "JSON_PATH_EXISTS",
            "JSON_PATH_IN",
            "JSON_PATH_NUMBER_RANGE",
            "JSON_PATH_REGEX",
            "JSON_PATH_CONTAINS",
            "JSON_PATH_NOT_CONTAINS"
    );

    public void validateFormalEvalCase(EvalCaseRecord record) {
        validateJudgeRule(record);
        validateHardChecks(record);
    }

    public void validateFormalEvalCase(EvalCaseRecord record, WorkflowNodeDefinition node) {
        validateFormalEvalCase(record);
        if (containsSchemaValidationHardCheck(record.hardChecksJson()) && node.getOutputSchemaRef() == null) {
            throw new BizException(
                    ErrorCode.INVALID_ARGUMENT,
                    "SCHEMA_VALIDATION hardCheck \u8981\u6c42\u76ee\u6807\u8282\u70b9\u914d\u7f6e outputSchema\uff1a" + record.caseNo()
            );
        }
    }

    private void validateJudgeRule(EvalCaseRecord record) {
        String judgeRule = record.judgeRuleText();
        if (judgeRule == null || judgeRule.isBlank()) {
            throw new BizException(
                    ErrorCode.INVALID_ARGUMENT,
                    "\u6b63\u5f0f\u9a8c\u6536\u7528\u4f8b\u786e\u8ba4\u524d\u5fc5\u987b\u586b\u5199 judgeRule\uff1a" + record.caseNo()
            );
        }
    }

    private void validateHardChecks(EvalCaseRecord record) {
        JsonNode hardChecks = record.hardChecksJson();
        if (hardChecks == null || hardChecks.isNull() || hardChecks.isMissingNode() || !hardChecks.isArray()) {
            throw new BizException(
                    ErrorCode.INVALID_ARGUMENT,
                    "\u6b63\u5f0f\u9a8c\u6536\u7528\u4f8b\u7684 hardChecks \u5fc5\u987b\u662f\u6570\u7ec4\uff1a" + record.caseNo()
            );
        }
        for (int index = 0; index < hardChecks.size(); index++) {
            validateHardCheck(record, index, hardChecks.get(index));
        }
    }

    private void validateHardCheck(EvalCaseRecord record, int index, JsonNode hardCheck) {
        if (hardCheck == null || !hardCheck.isObject()) {
            throw new BizException(
                    ErrorCode.INVALID_ARGUMENT,
                    "\u6b63\u5f0f\u9a8c\u6536\u7528\u4f8b\u7b2c " + (index + 1) + " \u6761 hardCheck \u5fc5\u987b\u662f\u5bf9\u8c61\uff1a" + record.caseNo()
            );
        }
        String type = requireTextField(record, index, hardCheck, "type");
        if (!SUPPORTED_HARD_CHECK_TYPES.contains(type)) {
            throw new BizException(
                    ErrorCode.INVALID_ARGUMENT,
                    "\u6b63\u5f0f\u9a8c\u6536\u7528\u4f8b\u5305\u542b\u4e0d\u652f\u6301\u7684 hardCheck \u7c7b\u578b\uff1a" + type
            );
        }
        switch (type) {
            case "SCHEMA_VALIDATION" -> {
                return;
            }
            case "JSON_PATH_EXISTS" -> requirePath(record, index, hardCheck);
            case "JSON_PATH_IN" -> {
                requirePath(record, index, hardCheck);
                JsonNode values = hardCheck.get("values");
                if (values == null || !values.isArray() || values.isEmpty()) {
                    throw new BizException(
                            ErrorCode.INVALID_ARGUMENT,
                            "\u6b63\u5f0f\u9a8c\u6536\u7528\u4f8b\u7b2c " + (index + 1)
                                    + " \u6761 hardCheck \u7684 values \u5fc5\u987b\u662f\u975e\u7a7a\u6570\u7ec4\uff1a" + record.caseNo()
                    );
                }
            }
            case "JSON_PATH_NUMBER_RANGE" -> {
                requirePath(record, index, hardCheck);
                boolean hasMin = hardCheck.has("min") && hardCheck.get("min").isNumber();
                boolean hasMax = hardCheck.has("max") && hardCheck.get("max").isNumber();
                if (!hasMin && !hasMax) {
                    throw new BizException(
                            ErrorCode.INVALID_ARGUMENT,
                            "\u6b63\u5f0f\u9a8c\u6536\u7528\u4f8b\u7b2c " + (index + 1)
                                    + " \u6761 hardCheck \u81f3\u5c11\u9700\u8981\u4e00\u4e2a\u6570\u503c\u578b min \u6216 max\uff1a" + record.caseNo()
                    );
                }
                if (hardCheck.has("min") && !hardCheck.get("min").isNumber()) {
                    throw new BizException(
                            ErrorCode.INVALID_ARGUMENT,
                            "\u6b63\u5f0f\u9a8c\u6536\u7528\u4f8b\u7b2c " + (index + 1)
                                    + " \u6761 hardCheck \u7684 min \u5fc5\u987b\u662f number\uff1a" + record.caseNo()
                    );
                }
                if (hardCheck.has("max") && !hardCheck.get("max").isNumber()) {
                    throw new BizException(
                            ErrorCode.INVALID_ARGUMENT,
                            "\u6b63\u5f0f\u9a8c\u6536\u7528\u4f8b\u7b2c " + (index + 1)
                                    + " \u6761 hardCheck \u7684 max \u5fc5\u987b\u662f number\uff1a" + record.caseNo()
                    );
                }
            }
            case "JSON_PATH_REGEX" -> {
                requirePath(record, index, hardCheck);
                String pattern = requireTextField(record, index, hardCheck, "pattern");
                try {
                    Pattern.compile(pattern);
                } catch (PatternSyntaxException exception) {
                    throw new BizException(
                            ErrorCode.INVALID_ARGUMENT,
                            "\u6b63\u5f0f\u9a8c\u6536\u7528\u4f8b\u7b2c " + (index + 1)
                                    + " \u6761 hardCheck \u7684 pattern \u4e0d\u662f\u5408\u6cd5 Java \u6b63\u5219\uff1a" + record.caseNo()
                    );
                }
            }
            case "JSON_PATH_CONTAINS", "JSON_PATH_NOT_CONTAINS" -> {
                requirePath(record, index, hardCheck);
                requireTextField(record, index, hardCheck, "expected");
            }
            default -> throw new BizException(
                    ErrorCode.INVALID_ARGUMENT,
                    "\u4e0d\u652f\u6301\u7684 hardCheck \u7c7b\u578b\uff1a" + type
            );
        }
    }

    private String requirePath(EvalCaseRecord record, int index, JsonNode hardCheck) {
        String path = requireTextField(record, index, hardCheck, "path");
        if (!path.startsWith("$")) {
            throw new BizException(
                    ErrorCode.INVALID_ARGUMENT,
                    "\u6b63\u5f0f\u9a8c\u6536\u7528\u4f8b\u7b2c " + (index + 1)
                            + " \u6761 hardCheck \u7684 path \u5fc5\u987b\u4ee5 $ \u5f00\u5934\uff1a" + record.caseNo()
            );
        }
        return path;
    }

    private String requireTextField(EvalCaseRecord record, int index, JsonNode jsonNode, String fieldName) {
        JsonNode value = jsonNode.get(fieldName);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new BizException(
                    ErrorCode.INVALID_ARGUMENT,
                    "\u6b63\u5f0f\u9a8c\u6536\u7528\u4f8b\u7b2c " + (index + 1)
                            + " \u6761 hardCheck \u7f3a\u5c11\u5408\u6cd5\u7684 " + fieldName + "\uff1a" + record.caseNo()
            );
        }
        return value.asText();
    }

    private boolean containsSchemaValidationHardCheck(JsonNode hardChecks) {
        if (hardChecks == null || !hardChecks.isArray()) {
            return false;
        }
        for (JsonNode hardCheck : hardChecks) {
            if ("SCHEMA_VALIDATION".equals(hardCheck.path("type").asText())) {
                return true;
            }
        }
        return false;
    }
}
