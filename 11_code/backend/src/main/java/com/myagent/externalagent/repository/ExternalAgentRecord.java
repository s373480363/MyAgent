package com.myagent.externalagent.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.myagent.common.domain.EnableStatus;
import com.myagent.externalagent.domain.ExternalAgentType;

import java.time.Instant;

/**
 * 外部 Agent 持久化记录。
 *
 * @param id 主键
 * @param adapterKey 适配器标识
 * @param adapterType 适配器类型
 * @param name 名称
 * @param description 描述
 * @param commandJson 结构化命令配置
 * @param workingDirectory 工作目录
 * @param timeoutSeconds 超时时间
 * @param captureStdout 是否采集 stdout
 * @param captureStderr 是否采集 stderr
 * @param captureGitDiff 是否采集 Git diff
 * @param outputSchemaId 输出 Schema 主键
 * @param status 状态
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record ExternalAgentRecord(
        long id,
        String adapterKey,
        ExternalAgentType adapterType,
        String name,
        String description,
        JsonNode commandJson,
        String workingDirectory,
        int timeoutSeconds,
        boolean captureStdout,
        boolean captureStderr,
        boolean captureGitDiff,
        Long outputSchemaId,
        EnableStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
