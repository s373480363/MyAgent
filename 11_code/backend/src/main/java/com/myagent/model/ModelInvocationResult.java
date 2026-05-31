package com.myagent.model;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 模型调用结果。
 *
 * @param output 输出 JSON
 * @param rawText 原始文本
 * @param durationMs 耗时毫秒
 */
public record ModelInvocationResult(JsonNode output, String rawText, long durationMs) {
}
