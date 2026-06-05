package com.myagent.model;

import java.math.BigDecimal;

/**
 * 模型请求 Trace 白名单载荷。
 *
 * @param providerKey 供应商标识
 * @param providerName 供应商名称
 * @param modelOfferingKey 模型供应项标识
 * @param modelKey 模型身份键
 * @param upstreamModelName 上游模型名
 * @param temperature 最终温度
 * @param structuredOutput 是否结构化输出
 */
public record ModelRequestTracePayload(
        String providerKey,
        String providerName,
        String modelOfferingKey,
        String modelKey,
        String upstreamModelName,
        BigDecimal temperature,
        boolean structuredOutput
) {
}
