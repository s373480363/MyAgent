package com.myagent.run.application.command;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 调试运行 Agent 命令。
 *
 * @param agentId Agent 主键
 * @param workflowVersionId 工作流版本主键
 * @param input 输入 JSON
 */
public record RunDebugAgentCommand(long agentId, Long workflowVersionId, JsonNode input) {
}
