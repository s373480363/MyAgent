package com.myagent.modelcatalog.application.result;

import com.myagent.common.domain.EnableStatus;
import com.myagent.modelcatalog.domain.ModelProviderType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * 模型供应商结果。
 *
 * @param providerId 供应商主键
 * @param providerKey 供应商标识
 * @param name 名称
 * @param providerType 类型
 * @param baseUrl Base URL
 * @param apiKeyConfigured 是否已配置 API Key
 * @param apiKeyMask API Key 掩码
 * @param status 状态
 * @param description 描述
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
@Schema(name = "ModelProviderResult", description = "模型供应商结果。")
public record ModelProviderResult(
        long providerId,
        String providerKey,
        String name,
        ModelProviderType providerType,
        String baseUrl,
        boolean apiKeyConfigured,
        String apiKeyMask,
        EnableStatus status,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
}
