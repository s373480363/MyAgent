package com.myagent.workflow.application.command;

/**
 * 发布工作流草稿命令。
 *
 * @param agentId Agent 主键
 * @param publishMessage 发布说明
 */
public record PublishWorkflowDraftCommand(long agentId, String publishMessage) {
}
