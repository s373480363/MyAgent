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
 * Judge evaluator.
 */
@Component
public class DefaultEvalJudgeEvaluator implements EvalJudgeEvaluator {

    static final String JUDGE_PROMPT_VERSION = "JUDGE_RULE_V1";

    private final ObjectMapper objectMapper;
    private final OpenAiModelGateway modelGateway;

    public DefaultEvalJudgeEvaluator(ObjectMapper objectMapper, OpenAiModelGateway modelGateway) {
        this.objectMapper = objectMapper;
        this.modelGateway = modelGateway;
    }

    @Override
    public EvalJudgeEvaluation evaluate(EvalJudgeRequest request) {
        String judgeModelOfferingKey = request.judgeModelOfferingKey();
        if (judgeModelOfferingKey == null || judgeModelOfferingKey.isBlank()) {
            return new EvalJudgeEvaluation(null, null, null, null, "judge \u6a21\u578b\u4f9b\u5e94\u9879\u672a\u914d\u7f6e\u3002");
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
                        "judge LLM \u8f93\u51fa\u4e0d\u662f\u5408\u6cd5\u7684\u7ed3\u6784\u5316\u7ed3\u679c\u3002"
                );
            }
            String errorMessage = judgeResult.path("passed").asBoolean(false)
                    ? ""
                    : judgeResult.path("reason").asText("judge \u5224\u5b9a\u672a\u901a\u8fc7\u3002");
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
                    "judge LLM \u8c03\u7528\u5931\u8d25\uff1a" + exception.getMessage()
            );
        } catch (RuntimeException exception) {
            return new EvalJudgeEvaluation(
                    null,
                    null,
                    judgeModelOfferingKey,
                    JUDGE_PROMPT_VERSION,
                    "judge LLM \u8c03\u7528\u5931\u8d25\uff1a" + exception.getMessage()
            );
        }
    }

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

    private BigDecimal normalizeTemperature(BigDecimal temperature) {
        return temperature == null ? BigDecimal.ZERO : temperature;
    }

    private JsonNode nullToJsonNull(JsonNode value) {
        return value == null ? objectMapper.nullNode() : value;
    }
}
