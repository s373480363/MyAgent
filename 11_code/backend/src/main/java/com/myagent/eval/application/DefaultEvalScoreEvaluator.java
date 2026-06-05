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
 * 默认 Eval LLM 评分执行器。
 */
@Component
public class DefaultEvalScoreEvaluator implements EvalScoreEvaluator {

    /**
     * JSON 对象映射器。
     */
    private final ObjectMapper objectMapper;

    /**
     * 模型网关。
     */
    private final OpenAiModelGateway modelGateway;

    /**
     * 构造评分执行器。
     *
     * @param objectMapper JSON 对象映射器
     * @param modelGateway 模型网关
     */
    public DefaultEvalScoreEvaluator(ObjectMapper objectMapper, OpenAiModelGateway modelGateway) {
        this.objectMapper = objectMapper;
        this.modelGateway = modelGateway;
    }

    /**
     * 执行可选 LLM 评分。
     *
     * @param request 评分请求
     * @return 评分结果；未配置评分规则时返回 null
     */
    @Override
    public JsonNode evaluate(EvalScoreRequest request) {
        if (request.scoreRule() == null || request.scoreRule().isNull()
                || (request.scoreRule().isObject() && request.scoreRule().isEmpty())) {
            return null;
        }
        if (!request.scoreRule().path("enabled").asBoolean(false)) {
            return null;
        }
        ObjectNode promptPayload = objectMapper.createObjectNode();
        promptPayload.set("scoreRule", request.scoreRule());
        promptPayload.set("input", nullToJsonNull(request.input()));
        promptPayload.set("referenceAnswer", nullToJsonNull(request.referenceAnswer()));
        promptPayload.set("output", nullToJsonNull(request.output()));
        promptPayload.set("assertionResults", nullToJsonNull(request.assertionResults()));
        promptPayload.put("assertionPassed", request.assertionPassed());
        String modelOfferingKey = readText(request.scoreRule(), "modelOfferingKey", request.agent().defaultModelOfferingKey());
        if (modelOfferingKey == null || modelOfferingKey.isBlank()) {
            return objectMapper.createObjectNode()
                    .put("scored", true)
                    .put("status", "FAILED")
                    .put("errorMessage", "LLM 评分未配置模型供应项，且 Agent 默认模型供应项为空。");
        }
        String prompt = readText(
                request.scoreRule(),
                "promptTemplate",
                "请根据 scoreRule、referenceAnswer、output 和 assertionResults 给出辅助评分 JSON。输入如下：{payload}"
        ).replace("{payload}", promptPayload.toString());
        try {
            ModelInvocationResult result = modelGateway.invoke(new ModelInvocationRequest(
                    modelOfferingKey,
                    "你是节点验收辅助评分器。只输出 JSON 评分结论，不能覆盖确定性断言结果。",
                    prompt,
                    promptPayload,
                    readDecimal(request.scoreRule(), "temperature", BigDecimal.ZERO),
                    true
            ));
            ObjectNode scoreResult = objectMapper.createObjectNode()
                    .put("scored", true)
                    .put("status", "SUCCESS")
                    .put("modelOfferingKey", modelOfferingKey)
                    .put("rawText", result.rawText())
                    .put("durationMs", result.durationMs());
            scoreResult.set("output", nullToJsonNull(result.output()));
            return scoreResult;
        } catch (BizException exception) {
            return objectMapper.createObjectNode()
                    .put("scored", true)
                    .put("status", "FAILED")
                    .put("modelOfferingKey", modelOfferingKey)
                    .put("errorCode", exception.getErrorCode().getCode())
                    .put("errorMessage", exception.getMessage());
        } catch (RuntimeException exception) {
            return objectMapper.createObjectNode()
                    .put("scored", true)
                    .put("status", "FAILED")
                    .put("modelOfferingKey", modelOfferingKey)
                    .put("errorMessage", "LLM 评分失败：" + exception.getMessage());
        }
    }

    /**
     * 读取文本字段。
     *
     * @param jsonNode JSON 对象
     * @param fieldName 字段名
     * @param fallback 默认值
     * @return 文本值
     */
    private String readText(JsonNode jsonNode, String fieldName, String fallback) {
        JsonNode value = jsonNode == null ? null : jsonNode.get(fieldName);
        if (value != null && value.isTextual() && !value.asText().isBlank()) {
            return value.asText();
        }
        return fallback;
    }

    /**
     * 读取数值字段。
     *
     * @param jsonNode JSON 对象
     * @param fieldName 字段名
     * @param fallback 默认值
     * @return 数值
     */
    private BigDecimal readDecimal(JsonNode jsonNode, String fieldName, BigDecimal fallback) {
        JsonNode value = jsonNode == null ? null : jsonNode.get(fieldName);
        if (value != null && value.isNumber()) {
            return value.decimalValue();
        }
        return fallback;
    }

    /**
     * 将空引用转换为 JSON null。
     *
     * @param value 原始 JSON
     * @return 非空 JSON
     */
    private JsonNode nullToJsonNull(JsonNode value) {
        return value == null ? objectMapper.nullNode() : value;
    }
}
