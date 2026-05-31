package com.myagent.externalagent.application.result;

import com.myagent.common.domain.EnableStatus;
import com.myagent.externalagent.domain.ExternalAgentType;

import java.time.Instant;

/**
 * 外部 Agent 列表项结果。
 *
 * @param id 主键
 * @param adapterKey 适配器标识
 * @param adapterType 适配器类型
 * @param name 名称
 * @param description 描述
 * @param timeoutSeconds 超时时间
 * @param outputSchemaId 输出 Schema 主键
 * @param status 状态
 * @param updatedAt 更新时间
 */
public record ExternalAgentListItemResult(
        long id,
        String adapterKey,
        ExternalAgentType adapterType,
        String name,
        String description,
        int timeoutSeconds,
        Long outputSchemaId,
        EnableStatus status,
        Instant updatedAt
) {
}
