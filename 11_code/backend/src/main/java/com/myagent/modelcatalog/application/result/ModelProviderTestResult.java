package com.myagent.modelcatalog.application.result;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 模型供应商连接测试结果。
 *
 * @param providerKey 供应商标识
 * @param offeringKey 供应项标识
 * @param modelKey 模型身份键
 * @param upstreamModelName 上游模型名
 * @param durationMs 耗时毫秒
 * @param message 中文摘要
 */
@Schema(name = "ModelProviderTestResult", description = "模型供应商连接测试结果。")
public record ModelProviderTestResult(
        String providerKey,
        String offeringKey,
        String modelKey,
        String upstreamModelName,
        long durationMs,
        String message
) {
}
