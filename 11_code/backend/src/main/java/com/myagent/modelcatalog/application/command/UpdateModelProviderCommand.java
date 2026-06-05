package com.myagent.modelcatalog.application.command;

/**
 * 更新模型供应商命令。
 *
 * @param providerId 供应商主键
 * @param name 名称
 * @param baseUrl Base URL
 * @param description 描述
 */
public record UpdateModelProviderCommand(
        long providerId,
        String name,
        String baseUrl,
        String description
) {
}
