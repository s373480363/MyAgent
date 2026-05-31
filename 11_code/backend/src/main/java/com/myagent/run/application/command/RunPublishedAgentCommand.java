package com.myagent.run.application.command;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 正式运行 Agent 命令。
 *
 * @param agentKey Agent 业务标识
 * @param input 输入 JSON
 */
public record RunPublishedAgentCommand(String agentKey, JsonNode input) {
}
