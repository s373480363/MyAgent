package com.myagent.modelcatalog.application.command;

import java.math.BigDecimal;

/**
 * 更新模型供应项命令。
 *
 * @param offeringId 供应项主键
 * @param providerKey 供应商标识
 * @param modelKey 模型身份键
 * @param displayName 展示名称
 * @param upstreamModelName 上游模型名
 * @param defaultTemperature 默认温度
 * @param description 描述
 */
public record UpdateModelOfferingCommand(
        long offeringId,
        String providerKey,
        String modelKey,
        String displayName,
        String upstreamModelName,
        BigDecimal defaultTemperature,
        String description
) {
}
