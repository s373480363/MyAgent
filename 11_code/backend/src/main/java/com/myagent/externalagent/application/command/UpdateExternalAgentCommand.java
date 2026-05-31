package com.myagent.externalagent.application.command;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * 更新外部 Agent 命令。
 *
 * @param adapterId 外部 Agent 主键
 * @param name 名称
 * @param description 描述
 * @param commandJson 结构化命令配置
 * @param secretHeaders 敏感 header 定义
 * @param workingDirectory 工作目录
 * @param timeoutSeconds 超时时间
 * @param captureStdout 是否采集 stdout
 * @param captureStderr 是否采集 stderr
 * @param captureGitDiff 是否采集 Git diff
 * @param outputSchemaId 输出 Schema 主键
 */
public record UpdateExternalAgentCommand(
        long adapterId,
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
     * 敏感 header 定义项。
     *
     * @param headerName header 名称
     * @param secretValue 禁止通过普通更新接口提交的 secret 值
     */
    public record SecretHeaderItem(String headerName, String secretValue) {
    }
}
