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
                        """)
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
                objectMapper.readTree("[{\"type\":\"JSON_PATH_EXISTS\",\"path\":\"$.summary\"}]")
        );

        assertThat(evaluation.passed()).isFalse();
        assertThat(evaluation.errorMessage()).isEqualTo("$.summary 字段缺失。");
    }
}
