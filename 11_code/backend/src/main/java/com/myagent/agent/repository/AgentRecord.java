package com.myagent.agent.repository;

import com.myagent.common.domain.EnableStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Agent 持久化记录。
 *
 * @param id 主键
 * @param agentKey Agent 业务标识
 * @param name 名称
 * @param description 描述
 * @param status 状态
 * @param systemPrompt 系统提示词
 * @param defaultModelOfferingKey 默认模型供应项标识
 * @param temperature 温度
 * @param timeoutSeconds Agent 默认总超时
 * @param maxSteps Agent 默认最大步数
 * @param currentDraftWorkflowVersionId 当前草稿版本主键
 * @param currentPublishedWorkflowVersionId 当前发布版本主键
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record AgentRecord(
        long id,
        String agentKey,
        String name,
        String description,
        EnableStatus status,
        String systemPrompt,
        String defaultModelOfferingKey,
        BigDecimal temperature,
        int timeoutSeconds,
        int maxSteps,
        Long currentDraftWorkflowVersionId,
        Long currentPublishedWorkflowVersionId,
        Instant createdAt,
        Instant updatedAt
) {
}
