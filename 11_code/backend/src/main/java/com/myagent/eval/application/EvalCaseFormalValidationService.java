package com.myagent.eval.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.myagent.common.error.BizException;
import com.myagent.common.error.ErrorCode;
import com.myagent.eval.repository.EvalCaseRecord;
import com.myagent.workflow.domain.WorkflowNodeDefinition;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 正式验收用例校验服务，集中维护用例进入正式运行前的断言契约。
 */
@Component
public class EvalCaseFormalValidationService {

    /**
     * V1 正式支持的确定性断言类型。
     */
    private static final Set<String> SUPPORTED_ASSERTION_TYPES = Set.of(
            "JSON_PATH_EXISTS",
            "JSON_PATH_EQUALS",
            "JSON_PATH_CONTAINS",
            "JSON_PATH_NOT_CONTAINS",
            "JSON_PATH_REGEX",
            "JSON_PATH_NUMBER_RANGE",
            "JSON_PATH_IN",
            "SCHEMA_VALIDATION"
    );

    /**
     * 必须显式提供 expected 字段的断言类型。
     */
    private static final Set<String> EXPECTED_ASSERTION_TYPES = Set.of(
            "JSON_PATH_EQUALS",
            "JSON_PATH_CONTAINS",
            "JSON_PATH_NOT_CONTAINS"
    );

    /**
     * 校验正式 EvalCase 是否具备可执行验收标准。
     *
     * @param record EvalCase 记录
     */
    public void validateFormalEvalCase(EvalCaseRecord record) {
        JsonNode assertions = record.assertionsJson();
        if (assertions == null || assertions.isNull() || assertions.isMissingNode() || !assertions.isArray()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "正式验收用例必须配置断言数组：" + record.caseNo());
        }
        if (assertions.isEmpty()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "正式验收用例断言不能为空：" + record.caseNo());
        }
        for (int index = 0; index < assertions.size(); index++) {
            JsonNode assertion = assertions.get(index);
            if (assertion == null || !assertion.isObject()) {
                throw new BizException(ErrorCode.INVALID_ARGUMENT, "正式验收用例第 " + (index + 1) + " 条断言必须是对象：" + record.caseNo());
            }
            JsonNode typeNode = assertion.get("type");
            if (typeNode == null || !typeNode.isTextual() || typeNode.asText().isBlank()) {
                throw new BizException(ErrorCode.INVALID_ARGUMENT, "正式验收用例第 " + (index + 1) + " 条断言缺少 type：" + record.caseNo());
            }
            String type = typeNode.asText();
            if (!SUPPORTED_ASSERTION_TYPES.contains(type)) {
                throw new BizException(ErrorCode.INVALID_ARGUMENT, "正式验收用例不支持断言类型：" + type);
            }
            validateAssertionFields(record, index, type, assertion);
        }
    }

    /**
     * 校验断言字段契约，禁止历史字段进入正式验收运行。
     *
     * @param record EvalCase 记录
     * @param index 断言下标
     * @param type 断言类型
     * @param assertion 断言定义
     */
    private void validateAssertionFields(EvalCaseRecord record, int index, String type, JsonNode assertion) {
        if (assertion.has("value")) {
            throw new BizException(
                    ErrorCode.INVALID_ARGUMENT,
                    "正式验收用例第 " + (index + 1) + " 条断言不支持 value 字段，请使用 expected：" + record.caseNo()
            );
        }
        if (EXPECTED_ASSERTION_TYPES.contains(type) && !assertion.has("expected")) {
            throw new BizException(
                    ErrorCode.INVALID_ARGUMENT,
                    "正式验收用例第 " + (index + 1) + " 条断言缺少 expected：" + record.caseNo()
            );
        }
    }

    /**
     * 结合目标节点校验正式 EvalCase。
     *
     * @param record EvalCase 记录
     * @param node 目标节点
     */
    public void validateFormalEvalCase(EvalCaseRecord record, WorkflowNodeDefinition node) {
        validateFormalEvalCase(record);
        if (containsSchemaValidationAssertion(record.assertionsJson()) && node.getOutputSchemaRef() == null) {
            throw new BizException(
                    ErrorCode.INVALID_ARGUMENT,
                    "SCHEMA_VALIDATION 断言要求目标节点配置 outputSchema：" + record.caseNo()
            );
        }
    }

    /**
     * 判断断言列表是否包含 Schema 校验断言。
     *
     * @param assertions 断言配置
     * @return 包含 Schema 校验断言时返回 true
     */
    private boolean containsSchemaValidationAssertion(JsonNode assertions) {
        if (assertions == null || !assertions.isArray()) {
            return false;
        }
        for (JsonNode assertion : assertions) {
            if ("SCHEMA_VALIDATION".equals(assertion.path("type").asText())) {
                return true;
            }
        }
        return false;
    }
}
