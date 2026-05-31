package com.myagent.externalagent.application.command;

import com.fasterxml.jackson.databind.JsonNode;
import com.myagent.externalagent.domain.ExternalAgentType;

import java.util.List;

/**
 * 创建外部 Agent 命令。
 *
 * @param adapterKey 适配器标识
 * @param adapterType 适配器类型
 * @param name 名称
 * @param description 描述
 * @param commandJson 结构化命令配置
 * @param secretHeaders 敏感 header 定义和可选初始 secret
 * @param workingDirectory 工作目录
 * @param timeoutSeconds 超时时间
 * @param captureStdout 是否采集 stdout
 * @param captureStderr 是否采集 stderr
 * @param captureGitDiff 是否采集 Git diff
 * @param outputSchemaId 输出 Schema 主键
 */
public record CreateExternalAgentCommand(
        String adapterKey,
        ExternalAgentType adapterType,
        String name,
        String description,
        JsonNode commandJson,
        List<SecretHeaderItem> secretHeaders,
        String workingDirectory,
        Integer timeoutSeconds,
        Boolean captureStdout,
        Boolean captureStderr,
        Boolean captureGitDiff,
        Long outputSchemaId
) {

    /**
     * 敏感 header 请求项。
     *
     * @param headerName header 名称
     * @param secretValue 可选 secret 值
     */
    public record SecretHeaderItem(String headerName, String secretValue) {
    }
}
