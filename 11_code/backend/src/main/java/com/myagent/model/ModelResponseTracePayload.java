package com.myagent.model;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 模型响应 Trace 白名单载荷。
 *
 * @param output 输出 JSON
 * @param rawText 原始文本
 * @param durationMs 耗时毫秒
 */
public record ModelResponseTracePayload(
        JsonNode output,
        String rawText,
        long durationMs
) {

    /**
     * 根据模型调用结果构造响应 Trace 载荷。
     *
     * @param result 模型调用结果
     * @return Trace 载荷
     */
    public static ModelResponseTracePayload from(ModelInvocationResult result) {
        return new ModelResponseTracePayload(result.output(), result.rawText(), result.durationMs());
    }
}
