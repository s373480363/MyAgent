package com.myagent.modelcatalog.application.command;

import com.myagent.modelcatalog.domain.ModelProviderType;

/**
 * 创建模型供应商命令。
 *
 * @param providerKey 供应商标识
 * @param name 名称
 * @param providerType 类型
 * @param baseUrl Base URL
 * @param apiKey API Key
 * @param description 描述
 */
public record CreateModelProviderCommand(
        String providerKey,
        String name,
        ModelProviderType providerType,
        String baseUrl,
        String apiKey,
        String description
) {
}
