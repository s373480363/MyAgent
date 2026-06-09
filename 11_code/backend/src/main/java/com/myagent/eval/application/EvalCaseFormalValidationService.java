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
 * 正式验收用例校验服务。
 */
@Component
public class EvalCaseFormalValidationService {

    /**
     * V1 允许的 hardCheck 类型集合。
     */
    private static final Set<String> SUPPORTED_HARD_CHECK_TYPES = Set.of(
            "SCHEMA_VALIDATION",
            "JSON_PATH_EXISTS",
            "JSON_PATH_IN",
            "JSON_PATH_NUMBER_RANGE",
            "JSON_PATH_REGEX",
            "JSON_PATH_CONTAINS",
            "JSON_PATH_NOT_CONTAINS"
    );

    /**
     * 校验正式验收用例的基础契约。
     *
     * @param record 验收用例记录
     */
    public void validateFormalEvalCase(EvalCaseRecord record) {
        validateJudgeRule(record);
        validateHardChecks(record);
    }

    /**
     * 结合目标节点信息校验正式验收用例。
     *
     * @param record 验收用例记录
     * @param node 目标节点定义
     */
    public void validateFormalEvalCase(EvalCaseRecord record, WorkflowNodeDefinition node) {
        validateFormalEvalCase(record);
        if (containsSchemaValidationHardCheck(record.hardChecksJson()) && node.getOutputSchemaRef() == null) {
            throw new BizException(
                    ErrorCode.INVALID_ARGUMENT,
                    "SCHEMA_VALIDATION hardCheck 要求目标节点配置 outputSchema：" + record.caseNo()
            );
        }
    }

    /**
     * 校验正式验收用例的 judgeRule。
     *
     * @param record 验收用例记录
     */
    private void validateJudgeRule(EvalCaseRecord record) {
        String judgeRule = record.judgeRuleText();
        if (judgeRule == null || judgeRule.isBlank()) {
            throw new BizException(
                    ErrorCode.INVALID_ARGUMENT,
                    "正式验收用例确认前必须填写 judgeRule：" + record.caseNo()
            );
        }
    }

    /**
     * 校验正式验收用例的 hardChecks 数组。
     *
     * @param record 验收用例记录
     */
    private void validateHardChecks(EvalCaseRecord record) {
        JsonNode hardChecks = record.hardChecksJson();
        if (hardChecks == null || hardChecks.isNull() || hardChecks.isMissingNode() || !hardChecks.isArray()) {
            throw new BizException(
                    ErrorCode.INVALID_ARGUMENT,
                    "正式验收用例的 hardChecks 必须是数组：" + record.caseNo()
            );
        }
        for (int index = 0; index < hardChecks.size(); index++) {
            validateHardCheck(record, index, hardChecks.get(index));
        }
    }

    /**
     * 校验单条 hardCheck 配置。
     *
     * @param record 验收用例记录
     * @param index 当前 hardCheck 下标
     * @param hardCheck 单条 hardCheck 配置
     */
    private void validateHardCheck(EvalCaseRecord record, int index, JsonNode hardCheck) {
        if (hardCheck == null || !hardCheck.isObject()) {
            throw new BizException(
                    ErrorCode.INVALID_ARGUMENT,
                    "正式验收用例第 " + (index + 1) + " 条 hardCheck 必须是对象：" + record.caseNo()
            );
        }
        String type = requireTextField(record, index, hardCheck, "type");
        if (!SUPPORTED_HARD_CHECK_TYPES.contains(type)) {
            throw new BizException(
                    ErrorCode.INVALID_ARGUMENT,
                    "正式验收用例包含不支持的 hardCheck 类型：" + type
            );
        }
        // V1 这里校验的是冻结后的 hardChecks 契约，不允许开发自行扩展旧 assertions 字段语义。
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
                            "正式验收用例第 " + (index + 1)
                                    + " 条 hardCheck 的 values 必须是非空数组：" + record.caseNo()
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
                            "正式验收用例第 " + (index + 1)
                                    + " 条 hardCheck 至少需要一个数值型 min 或 max：" + record.caseNo()
                    );
                }
                if (hardCheck.has("min") && !hardCheck.get("min").isNumber()) {
                    throw new BizException(
                            ErrorCode.INVALID_ARGUMENT,
                            "正式验收用例第 " + (index + 1)
                                    + " 条 hardCheck 的 min 必须是 number：" + record.caseNo()
                    );
                }
                if (hardCheck.has("max") && !hardCheck.get("max").isNumber()) {
                    throw new BizException(
                            ErrorCode.INVALID_ARGUMENT,
                            "正式验收用例第 " + (index + 1)
                                    + " 条 hardCheck 的 max 必须是 number：" + record.caseNo()
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
                            "正式验收用例第 " + (index + 1)
                                    + " 条 hardCheck 的 pattern 不是合法 Java 正则：" + record.caseNo()
                    );
                }
            }
            case "JSON_PATH_CONTAINS", "JSON_PATH_NOT_CONTAINS" -> {
                requirePath(record, index, hardCheck);
                requireTextField(record, index, hardCheck, "expected");
            }
            default -> throw new BizException(
                    ErrorCode.INVALID_ARGUMENT,
                    "不支持的 hardCheck 类型：" + type
            );
        }
    }

    /**
     * 校验并返回 hardCheck 的 JSONPath。
     *
     * @param record 验收用例记录
     * @param index 当前 hardCheck 下标
     * @param hardCheck 单条 hardCheck 配置
     * @return 合法 JSONPath
     */
    private String requirePath(EvalCaseRecord record, int index, JsonNode hardCheck) {
        String path = requireTextField(record, index, hardCheck, "path");
        if (!path.startsWith("$")) {
            throw new BizException(
                    ErrorCode.INVALID_ARGUMENT,
                    "正式验收用例第 " + (index + 1)
                            + " 条 hardCheck 的 path 必须以 $ 开头：" + record.caseNo()
            );
        }
        return path;
    }

    /**
     * 校验并返回文本字段。
     *
     * @param record 验收用例记录
     * @param index 当前 hardCheck 下标
     * @param jsonNode hardCheck 配置
     * @param fieldName 字段名
     * @return 非空文本值
     */
    private String requireTextField(EvalCaseRecord record, int index, JsonNode jsonNode, String fieldName) {
        JsonNode value = jsonNode.get(fieldName);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new BizException(
                    ErrorCode.INVALID_ARGUMENT,
                    "正式验收用例第 " + (index + 1)
                            + " 条 hardCheck 缺少合法的 " + fieldName + "：" + record.caseNo()
            );
        }
        return value.asText();
    }

    /**
     * 判断 hardChecks 中是否包含 SCHEMA_VALIDATION。
     *
     * @param hardChecks hardChecks 配置数组
     * @return 是否包含 SCHEMA_VALIDATION
     */
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
