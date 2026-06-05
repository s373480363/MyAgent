package com.myagent.modelcatalog.repository;

import com.myagent.common.domain.EnableStatus;
import com.myagent.modelcatalog.domain.ModelProviderType;

import java.time.Instant;

/**
 * 模型供应商持久化记录。
 *
 * @param id 主键
 * @param providerKey 供应商标识
 * @param name 名称
 * @param providerType 类型
 * @param baseUrl Base URL
 * @param apiKeyCiphertext API Key 密文
 * @param apiKeyMask API Key 掩码
 * @param status 状态
 * @param description 描述
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record ModelProviderRecord(
        long id,
        String providerKey,
        String name,
        ModelProviderType providerType,
        String baseUrl,
        String apiKeyCiphertext,
        String apiKeyMask,
        EnableStatus status,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
}
