package com.myagent.modelcatalog.repository;

import com.myagent.common.domain.EnableStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 模型供应项持久化记录。
 *
 * @param id 主键
 * @param offeringKey 供应项标识
 * @param providerKey 供应商标识
 * @param modelKey 模型身份键
 * @param displayName 展示名称
 * @param upstreamModelName 上游模型名
 * @param defaultTemperature 默认温度
 * @param status 状态
 * @param description 描述
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record ModelOfferingRecord(
        long id,
        String offeringKey,
        String providerKey,
        String modelKey,
        String displayName,
        String upstreamModelName,
        BigDecimal defaultTemperature,
        EnableStatus status,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
}
