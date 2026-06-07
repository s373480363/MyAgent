package com.myagent.eval.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myagent.common.error.BizException;
import com.myagent.eval.domain.EvalCaseConfirmStatus;
import com.myagent.eval.repository.EvalCaseRecord;
import com.myagent.workflow.domain.WorkflowNodeDefinition;
import com.myagent.workflow.domain.WorkflowNodeType;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * EvalCase 正式校验服务测试。
 */
class EvalCaseFormalValidationServiceTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final EvalCaseFormalValidationService service = new EvalCaseFormalValidationService();

    @Test
    void validateFormalEvalCaseRejectsMissingJudgeRule() throws Exception {
        assertThatThrownBy(() -> service.validateFormalEvalCase(record("", """
                [
                  {"type": "JSON_PATH_EXISTS", "path": "$.summary"}
                ]
                """)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("judgeRule");
    }

    @Test
    void validateFormalEvalCaseRejectsUnsupportedHardCheckType() throws Exception {
        assertThatThrownBy(() -> service.validateFormalEvalCase(record("必须包含 summary。", """
                [
                  {"type": "JSON_PATH_EQUALS", "path": "$.summary", "expected": "ok"}
                ]
                """)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("JSON_PATH_EQUALS");
    }

    @Test
    void validateFormalEvalCaseRejectsInvalidRegexPattern() throws Exception {
        assertThatThrownBy(() -> service.validateFormalEvalCase(record("编号必须合法。", """
                [
                  {"type": "JSON_PATH_REGEX", "path": "$.orderNo", "pattern": "["}
                ]
                """)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("pattern");
    }

    @Test
    void validateFormalEvalCaseRejectsSchemaValidationWithoutOutputSchema() throws Exception {
        WorkflowNodeDefinition node = new WorkflowNodeDefinition();
        node.setNodeId("llm");
        node.setName("LLM");
        node.setType(WorkflowNodeType.LLM);

        assertThatThrownBy(() -> service.validateFormalEvalCase(record("输出必须满足 schema。", """
                [
                  {"type": "SCHEMA_VALIDATION"}
                ]
                """), node))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("outputSchema");
    }

    private EvalCaseRecord record(String judgeRule, String hardChecksJson) throws Exception {
        return new EvalCaseRecord(
                1L,
                2L,
                "case-001",
                "测试用例",
                objectMapper.readTree("""
                        {
                          "summary": "ok"
                        }
                        """),
                objectMapper.readTree("""
                        {
                          "summary": "ok"
                        }
                        """),
                judgeRule,
                objectMapper.readTree(hardChecksJson),
                true,
                EvalCaseConfirmStatus.USER_CONFIRMED,
                null,
                null,
                null,
                null,
                "描述",
                Instant.now(),
                Instant.now()
        );
    }
}
