package com.myagent.eval.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myagent.agent.repository.AgentRecord;
import com.myagent.common.domain.EnableStatus;
import com.myagent.common.error.BizException;
import com.myagent.common.error.ErrorCode;
import com.myagent.model.ModelInvocationResult;
import com.myagent.model.OpenAiModelGateway;
import com.myagent.workflow.domain.WorkflowNodeDefinition;
import com.myagent.workflow.domain.WorkflowNodeType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 默认 Eval 评分执行器测试。
 */
class DefaultEvalScoreEvaluatorTests {

    /**
     * JSON 对象映射器。
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 空评分规则不应调用模型。
     */
    @Test
    void evaluateReturnsNullWhenScoreRuleIsEmpty() {
        OpenAiModelGateway modelGateway = mock(OpenAiModelGateway.class);
        DefaultEvalScoreEvaluator evaluator = new DefaultEvalScoreEvaluator(objectMapper, modelGateway);

        JsonNode result = evaluator.evaluate(request(objectMapper.createObjectNode()));

        assertThat(result).isNull();
        verifyNoInteractions(modelGateway);
    }

    /**
     * 非空评分规则必须调用模型并返回评分结果。
     *
     * @throws Exception JSON 解析失败时抛出
     */
    @Test
    void evaluateInvokesModelWhenScoreRuleIsConfigured() throws Exception {
        OpenAiModelGateway modelGateway = mock(OpenAiModelGateway.class);
        DefaultEvalScoreEvaluator evaluator = new DefaultEvalScoreEvaluator(objectMapper, modelGateway);
        JsonNode scoreRule = objectMapper.readTree("""
                {
                  "enabled": true,
                  "modelOfferingKey": "openai.gpt_score",
                  "temperature": 0.2,
                  "promptTemplate": "评分输入 {payload}"
                }
                """);
        when(modelGateway.invoke(any())).thenReturn(new ModelInvocationResult(
                objectMapper.readTree("{\"score\":0.8,\"reason\":\"ok\"}"),
                "{\"score\":0.8,\"reason\":\"ok\"}",
                12
        ));

        JsonNode result = evaluator.evaluate(request(scoreRule));

        assertThat(result.get("scored").asBoolean()).isTrue();
        assertThat(result.get("status").asText()).isEqualTo("SUCCESS");
        assertThat(result.get("modelOfferingKey").asText()).isEqualTo("openai.gpt_score");
        assertThat(result.get("output").get("score").decimalValue()).isEqualByComparingTo("0.8");
        verify(modelGateway).invoke(argThat(request ->
                "openai.gpt_score".equals(request.modelOfferingKey())
                        && request.structuredOutput()
                        && request.userPrompt().contains("\"assertionPassed\":true")
        ));
    }

    /**
     * 模型评分失败时只记录评分失败，不向外抛出异常。
     *
     * @throws Exception JSON 解析失败时抛出
     */
    @Test
    void evaluateRecordsScoreFailureWithoutThrowing() throws Exception {
        OpenAiModelGateway modelGateway = mock(OpenAiModelGateway.class);
        DefaultEvalScoreEvaluator evaluator = new DefaultEvalScoreEvaluator(objectMapper, modelGateway);
        JsonNode scoreRule = objectMapper.readTree("{\"enabled\":true,\"modelOfferingKey\":\"openai.gpt_score\"}");
        when(modelGateway.invoke(any())).thenThrow(new BizException(ErrorCode.NODE_EXECUTION_FAILED, "模型不可用。"));

        JsonNode result = evaluator.evaluate(request(scoreRule));

        assertThat(result.get("scored").asBoolean()).isTrue();
        assertThat(result.get("status").asText()).isEqualTo("FAILED");
        assertThat(result.get("errorCode").asText()).isEqualTo(ErrorCode.NODE_EXECUTION_FAILED.getCode());
        assertThat(result.get("errorMessage").asText()).isEqualTo("模型不可用。");
    }

    /**
     * 构造评分请求。
     *
     * @param scoreRule 评分规则
     * @return 评分请求
     */
    private EvalScoreRequest request(JsonNode scoreRule) {
        return new EvalScoreRequest(
                scoreRule,
                agent(),
                node(),
                objectMapper.createObjectNode().put("input", "文本"),
                objectMapper.createObjectNode().put("expected", "答案"),
                objectMapper.createObjectNode().put("actual", "答案"),
                objectMapper.createArrayNode(),
                true
        );
    }

    /**
     * 构造 Agent 主数据。
     *
     * @return Agent 记录
     */
    private AgentRecord agent() {
        return new AgentRecord(
                1L,
                "agent",
                "Agent",
                "",
                EnableStatus.ENABLED,
                "系统提示词",
                "openai.gpt_default",
                BigDecimal.ZERO,
                600,
                30,
                10L,
                20L,
                Instant.now(),
                Instant.now()
        );
    }

    /**
     * 构造被验收节点。
     *
     * @return 工作流节点定义
     */
    private WorkflowNodeDefinition node() {
        WorkflowNodeDefinition node = new WorkflowNodeDefinition();
        node.setNodeId("llm");
        node.setName("模型节点");
        node.setType(WorkflowNodeType.LLM);
        return node;
    }
}
