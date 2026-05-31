package com.myagent.externalagent.application.command;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 测试外部 Agent 命令。
 *
 * @param adapterId 外部 Agent 主键
 * @param prompt 测试提示词
 * @param input 输入 JSON
 */
public record TestExternalAgentCommand(long adapterId, String prompt, JsonNode input) {
}
