package com.myagent.externalagent.application.result;

/**
 * 敏感 header 元信息结果。
 *
 * @param headerName header 名称
 * @param secretConfigured 是否已配置 secret
 */
public record ExternalAgentSecretHeaderResult(String headerName, boolean secretConfigured) {
}
