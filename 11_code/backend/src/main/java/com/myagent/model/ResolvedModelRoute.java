package com.myagent.model;

import java.math.BigDecimal;

/**
 * 已解析模型路由。
 *
 * @param providerId 供应商主键
 * @param providerKey 供应商标识
 * @param providerName 供应商名称
 * @param baseUrl Base URL
 * @param decryptedApiKey 解密后的 API Key
 * @param offeringId 供应项主键
 * @param offeringKey 供应项标识
 * @param modelKey 模型身份键
 * @param upstreamModelName 上游模型名
 * @param defaultTemperature 默认温度
 */
public record ResolvedModelRoute(
        long providerId,
        String providerKey,
        String providerName,
        String baseUrl,
        String decryptedApiKey,
        long offeringId,
        String offeringKey,
        String modelKey,
        String upstreamModelName,
        BigDecimal defaultTemperature
) {
}
