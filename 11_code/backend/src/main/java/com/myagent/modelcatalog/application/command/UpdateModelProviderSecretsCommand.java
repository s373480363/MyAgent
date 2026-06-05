package com.myagent.modelcatalog.application.command;

/**
 * 更新模型供应商密钥命令。
 *
 * @param providerId 供应商主键
 * @param apiKey 新密钥
 * @param clearApiKey 是否清空密钥
 */
public record UpdateModelProviderSecretsCommand(
        long providerId,
        String apiKey,
        boolean clearApiKey
) {
}
