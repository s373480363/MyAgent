package com.myagent.workflow.application.command;

/**
 * 校验工作流草稿命令。
 *
 * @param agentId Agent 主键
 */
public record ValidateWorkflowDraftCommand(long agentId) {
}
