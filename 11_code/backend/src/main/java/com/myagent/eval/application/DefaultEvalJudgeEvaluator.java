package com.myagent.eval.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.myagent.common.error.BizException;
import com.myagent.model.ModelInvocationRequest;
import com.myagent.model.ModelInvocationResult;
import com.myagent.model.OpenAiModelGateway;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 默认 judge 执行器。
 */
@Component
public class DefaultEvalJudgeEvaluator implements EvalJudgeEvaluator {

    /**
     * judge 提示词版本号。
     */
    static final String JUDGE_PROMPT_VERSION = "JUDGE_RULE_V1";

    /**
     * JSON 对象映射器。
     */
    private final ObjectMapper objectMapper;

    /**
     * 模型调用网关。
     */
    private final OpenAiModelGateway modelGateway;

    /**
     * 构造 judge 执行器。
     *
     * @param objectMapper JSON 对象映射器
     * @param modelGateway 模型调用网关
     */
    public DefaultEvalJudgeEvaluator(ObjectMapper objectMapper, OpenAiModelGateway modelGateway) {
        this.objectMapper = objectMapper;
        this.modelGateway = modelGateway;
    }

    /**
     * 调用 judge LLM 并校验返回结果结构。
     *
     * @param request judge 请求
     * @return judge 执行结果
     */
    @Override
    public EvalJudgeEvaluation evaluate(EvalJudgeRequest request) {
        String judgeModelOfferingKey = request.judgeModelOfferingKey();
        if (judgeModelOfferingKey == null || judgeModelOfferingKey.isBlank()) {
            return new EvalJudgeEvaluation(null, null, null, null, "judge 模型供应项未配置。");
        }

        ObjectNode promptPayload = objectMapper.createObjectNode();
        promptPayload.put("nodeId", request.node().getNodeId());
        promptPayload.put("nodeName", request.node().getName());
        promptPayload.put("nodeType", request.node().getType().name());
        promptPayload.put("judgeRule", request.judgeRule());
        promptPayload.set("input", nullToJsonNull(request.input()));
        promptPayload.set("referenceSample", nullToJsonNull(request.referenceSample()));
        promptPayload.set("output", nullToJsonNull(request.output()));
        promptPayload.set("hardCheckResults", nullToJsonNull(request.hardCheckResults()));

        String userPrompt = """
                Please evaluate the LLM node output using the given payload.
                Return JSON only.
                Required fields:
                1. passed: boolean
                2. reason: string
                3. score: number
                4. criteriaResults: array
                Each criteriaResults item must contain:
                - criterion: string
                - passed: boolean
                - reason: string
                payload:
                {payload}
                """.replace("{payload}", promptPayload.toString());

        try {
            // 先把 V1 契约需要的上下文拼进 payload，再把 payload 原样传给模型调用网关与 Trace。
            ModelInvocationResult result = modelGateway.invoke(new ModelInvocationRequest(
                    judgeModelOfferingKey,
                    "You are an eval judge. Follow judgeRule, referenceSample, actual output and hardCheckResults. Return JSON only.",
                    userPrompt,
                    promptPayload,
                    normalizeTemperature(request.judgeTemperature()),
                    true
            ));
            JsonNode judgeResult = validateJudgeResult(result.output());
            if (judgeResult == null) {
                return new EvalJudgeEvaluation(
                        null,
                        result.rawText(),
                        judgeModelOfferingKey,
                        JUDGE_PROMPT_VERSION,
                        "judge LLM 输出不是合法的结构化结果。"
                );
            }
            // 顶层失败摘要统一落到 errorMessage，具体原因继续保留在 judgeResult.reason。
            String errorMessage = judgeResult.path("passed").asBoolean(false)
                    ? ""
                    : judgeResult.path("reason").asText("judge 判定未通过。");
            return new EvalJudgeEvaluation(
                    judgeResult,
                    result.rawText(),
                    judgeModelOfferingKey,
                    JUDGE_PROMPT_VERSION,
                    errorMessage
            );
        } catch (BizException exception) {
            return new EvalJudgeEvaluation(
                    null,
                    null,
                    judgeModelOfferingKey,
                    JUDGE_PROMPT_VERSION,
                    "judge LLM 调用失败：" + exception.getMessage()
            );
        } catch (RuntimeException exception) {
            return new EvalJudgeEvaluation(
                    null,
                    null,
                    judgeModelOfferingKey,
                    JUDGE_PROMPT_VERSION,
                    "judge LLM 调用失败：" + exception.getMessage()
            );
        }
    }

    /**
     * 校验 judge LLM 返回的结构化结果是否符合 V1 最小契约。
     *
     * @param judgeResult judge LLM 输出
     * @return 合法结果；不合法时返回 null
     */
    private JsonNode validateJudgeResult(JsonNode judgeResult) {
        if (judgeResult == null || !judgeResult.isObject()) {
            return null;
        }
        JsonNode passed = judgeResult.get("passed");
        if (passed == null || !passed.isBoolean()) {
            return null;
        }
        JsonNode score = judgeResult.get("score");
        if (score != null && !score.isNull() && !score.isNumber()) {
            return null;
        }
        JsonNode reason = judgeResult.get("reason");
        if (reason != null && !reason.isNull() && !reason.isTextual()) {
            return null;
        }
        JsonNode criteriaResults = judgeResult.get("criteriaResults");
        if (criteriaResults != null && !criteriaResults.isNull() && !criteriaResults.isArray()) {
            return null;
        }
        return judgeResult;
    }

    /**
     * 规范化 judge 温度值。
     *
     * @param temperature 原始温度值
     * @return 规范化后的温度值
     */
    private BigDecimal normalizeTemperature(BigDecimal temperature) {
        return temperature == null ? BigDecimal.ZERO : temperature;
    }

    /**
     * 将空 JSON 值转换为显式 null 节点。
     *
     * @param value 原始 JSON 值
     * @return 非空 JSON 节点
     */
    private JsonNode nullToJsonNull(JsonNode value) {
        return value == null ? objectMapper.nullNode() : value;
    }
}
