package com.myagent.tool.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.myagent.common.domain.EnableStatus;

import java.time.Instant;

/**
 * 工具持久化记录。
 *
 * @param id 主键
 * @param toolKey 工具标识
 * @param name 工具名称
 * @param description 工具描述
 * @param inputSchemaId 输入 Schema 主键
 * @param outputSchemaId 输出 Schema 主键
 * @param executorType 执行器类型
 * @param executorConfigJson 执行器配置
 * @param status 状态
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record ToolRecord(
        long id,
        String toolKey,
        String name,
        String description,
        long inputSchemaId,
        long outputSchemaId,
        String executorType,
        JsonNode executorConfigJson,
        EnableStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
