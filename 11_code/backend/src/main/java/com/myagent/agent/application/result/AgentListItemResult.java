package com.myagent.agent.application.result;

import com.myagent.common.domain.EnableStatus;

import java.time.Instant;

/**
 * Agent 列表项。
 *
 * @param agentId Agent 主键
 * @param agentKey Agent 业务标识
 * @param name 名称
 * @param description 描述
 * @param status 状态
 * @param currentDraftWorkflowVersionId 当前草稿版本主键
 * @param currentPublishedWorkflowVersionId 当前发布版本主键
 * @param updatedAt 更新时间
 */
public record AgentListItemResult(
        long agentId,
        String agentKey,
        String name,
        String description,
        EnableStatus status,
        Long currentDraftWorkflowVersionId,
        Long currentPublishedWorkflowVersionId,
        Instant updatedAt
) {
}
