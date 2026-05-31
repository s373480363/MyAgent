package com.myagent.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;

/**
 * 模型调用请求。
 *
 * @param model 模型名称
 * @param systemPrompt 系统提示词
 * @param userPrompt 用户提示词
 * @param input 输入 JSON
 * @param temperature 温度
 * @param structuredOutput 是否结构化输出
 */
public record ModelInvocationRequest(
        String model,
        String systemPrompt,
        String userPrompt,
        JsonNode input,
        BigDecimal temperature,
        boolean structuredOutput
) {
}
