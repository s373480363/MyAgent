package com.myagent.modelcatalog.application.command;

import java.math.BigDecimal;

/**
 * 创建模型供应项命令。
 *
 * @param offeringKey 供应项标识
 * @param providerKey 供应商标识
 * @param modelKey 模型身份键
 * @param displayName 展示名称
 * @param upstreamModelName 上游模型名
 * @param defaultTemperature 默认温度
 * @param description 描述
 */
public record CreateModelOfferingCommand(
        String offeringKey,
        String providerKey,
        String modelKey,
        String displayName,
        String upstreamModelName,
        BigDecimal defaultTemperature,
        String description
) {
}
