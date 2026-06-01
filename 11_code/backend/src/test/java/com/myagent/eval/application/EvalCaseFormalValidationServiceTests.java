package com.myagent.eval.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myagent.common.error.BizException;
import com.myagent.eval.domain.EvalCaseConfirmStatus;
import com.myagent.eval.repository.EvalCaseRecord;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * EvalCase 正式化校验服务测试。
 */
class EvalCaseFormalValidationServiceTests {

    /**
     * JSON 对象映射器。
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * EvalCase 正式化校验服务。
     */
    private final EvalCaseFormalValidationService service = new EvalCaseFormalValidationService();

    /**
     * 正式断言类型不接受别名。
     *
     * @throws Exception JSON 解析失败时抛出
     */
    @Test
    void validateFormalEvalCaseRejectsAliasAssertionType() throws Exception {
        EvalCaseRecord record = new EvalCaseRecord(
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
                objectMapper.readTree("""
                        [
                          {"type": "FIELD_EQUALS", "path": "$.summary", "expected": "ok"}
                        ]
                        """),
                null,
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

        assertThatThrownBy(() -> service.validateFormalEvalCase(record))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不支持断言类型：FIELD_EQUALS");
    }

    /**
     * 正式断言不接受 value 作为 expected 的别名字段。
     *
     * @throws Exception JSON 解析失败时抛出
     */
    @Test
    void validateFormalEvalCaseRejectsValueAliasField() throws Exception {
        assertThatThrownBy(() -> service.validateFormalEvalCase(recordWithAssertions("""
                [
                  {"type": "JSON_PATH_EQUALS", "path": "$.summary", "value": "ok"}
                ]
                """)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不支持 value 字段");
    }

    /**
     * 需要期望值的断言必须显式配置 expected 字段。
     *
     * @throws Exception JSON 解析失败时抛出
     */
    @Test
    void validateFormalEvalCaseRequiresExpectedField() throws Exception {
        assertThatThrownBy(() -> service.validateFormalEvalCase(recordWithAssertions("""
                [
                  {"type": "JSON_PATH_EQUALS", "path": "$.summary"}
                ]
                """)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("缺少 expected");
    }

    /**
     * 构造带指定断言的 EvalCase。
     *
     * @param assertionsJson 断言 JSON
     * @return EvalCase 记录
     * @throws Exception JSON 解析失败时抛出
     */
    private EvalCaseRecord recordWithAssertions(String assertionsJson) throws Exception {
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
                objectMapper.readTree(assertionsJson),
                null,
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
