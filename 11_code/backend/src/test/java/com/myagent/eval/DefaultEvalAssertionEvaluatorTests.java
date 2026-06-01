package com.myagent.eval.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 默认验收断言执行器测试。
 */
class DefaultEvalAssertionEvaluatorTests {

    /**
     * JSON 对象映射器。
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 断言执行器。
     */
    private final DefaultEvalAssertionEvaluator evaluator = new DefaultEvalAssertionEvaluator(objectMapper);

    /**
     * 应按顺序执行确定性断言并返回字段级失败原因。
     *
     * @throws Exception JSON 解析失败时抛出
     */
    @Test
    void evaluateDeterministicAssertions() throws Exception {
        EvalAssertionEvaluation evaluation = evaluator.evaluate(
                objectMapper.readTree("""
                        {
                          "summary": "测试摘要",
                          "score": 90
                        }
                        """),
                objectMapper.readTree("""
                        [
                          {"type": "JSON_PATH_EXISTS", "path": "$.summary"},
                          {"type": "JSON_PATH_NUMBER_RANGE", "path": "$.score", "min": 80, "max": 100}
                        ]
                        """),
                null
        );

        assertThat(evaluation.passed()).isTrue();
        assertThat(evaluation.assertionResults()).hasSize(2);
    }

    /**
     * 断言失败时应保留第一条中文失败摘要。
     *
     * @throws Exception JSON 解析失败时抛出
     */
    @Test
    void evaluateFailureSummary() throws Exception {
        EvalAssertionEvaluation evaluation = evaluator.evaluate(
                objectMapper.readTree("{\"score\": 10}"),
                objectMapper.readTree("[{\"type\":\"JSON_PATH_EXISTS\",\"path\":\"$.summary\"}]"),
                null
        );

        assertThat(evaluation.passed()).isFalse();
        assertThat(evaluation.errorMessage()).isEqualTo("$.summary 字段缺失。");
    }

    /**
     * 空断言数组不能被隐式视为通过。
     */
    @Test
    void evaluateRejectsEmptyAssertions() {
        EvalAssertionEvaluation evaluation = evaluator.evaluate(
                objectMapper.createObjectNode().put("summary", "ok"),
                objectMapper.createArrayNode(),
                null
        );

        assertThat(evaluation.passed()).isFalse();
        assertThat(evaluation.errorMessage()).isEqualTo("断言配置不能为空。");
        assertThat(evaluation.assertionResults().get(0).path("type").asText()).isEqualTo("INVALID_ASSERTIONS");
    }

    /**
     * SCHEMA_VALIDATION 必须依赖真实的节点输出 Schema 校验结果。
     *
     * @throws Exception JSON 解析失败时抛出
     */
    @Test
    void schemaValidationAssertionRequiresOutputSchemaValidationResult() throws Exception {
        EvalAssertionEvaluation evaluation = evaluator.evaluate(
                objectMapper.createObjectNode().put("summary", "ok"),
                objectMapper.readTree("[{\"type\":\"SCHEMA_VALIDATION\"}]"),
                null
        );

        assertThat(evaluation.passed()).isFalse();
        assertThat(evaluation.errorMessage()).isEqualTo("未执行节点输出 Schema 校验。");
    }

    /**
     * SCHEMA_VALIDATION 在节点输出 Schema 校验成功后才通过。
     *
     * @throws Exception JSON 解析失败时抛出
     */
    @Test
    void schemaValidationAssertionPassesWithSuccessfulOutputSchemaValidation() throws Exception {
        EvalAssertionEvaluation evaluation = evaluator.evaluate(
                objectMapper.createObjectNode().put("summary", "ok"),
                objectMapper.readTree("[{\"type\":\"SCHEMA_VALIDATION\"}]"),
                objectMapper.readTree("""
                        {
                          "valid": true,
                          "results": [
                            {
                              "stage": "NODE_OUTPUT",
                              "valid": true,
                              "schemaKey": "summary-output",
                              "version": 1,
                              "errors": []
                            }
                          ]
                        }
                        """)
        );

        assertThat(evaluation.passed()).isTrue();
    }

    /**
     * 正式断言类型不接受别名写法。
     *
     * @throws Exception JSON 解析失败时抛出
     */
    @Test
    void evaluateRejectsAliasAssertionType() throws Exception {
        EvalAssertionEvaluation evaluation = evaluator.evaluate(
                objectMapper.createObjectNode().put("summary", "ok"),
                objectMapper.readTree("""
                        [
                          {"type": "FIELD_EQUALS", "path": "$.summary", "expected": "ok"}
                        ]
                        """),
                null
        );

        assertThat(evaluation.passed()).isFalse();
        assertThat(evaluation.errorMessage()).isEqualTo("不支持的断言类型：FIELD_EQUALS");
    }

    /**
     * value 不是正式期望值字段，不能被运行时当作 expected 兼容读取。
     *
     * @throws Exception JSON 解析失败时抛出
     */
    @Test
    void evaluateDoesNotReadValueAsExpectedFallback() throws Exception {
        EvalAssertionEvaluation evaluation = evaluator.evaluate(
                objectMapper.createObjectNode().put("summary", "ok"),
                objectMapper.readTree("""
                        [
                          {"type": "JSON_PATH_EQUALS", "path": "$.summary", "value": "ok"}
                        ]
                        """),
                null
        );

        assertThat(evaluation.passed()).isFalse();
        assertThat(evaluation.errorMessage()).isEqualTo("$.summary 不等于期望值。");
    }
}
