package com.myagent.workflow.application.command;

/**
 * 从已有版本复制生成新草稿命令。
 *
 * @param agentId Agent 主键
 * @param sourceWorkflowVersionId 来源版本主键
 */
public record CopyWorkflowDraftFromVersionCommand(long agentId, long sourceWorkflowVersionId) {
}
