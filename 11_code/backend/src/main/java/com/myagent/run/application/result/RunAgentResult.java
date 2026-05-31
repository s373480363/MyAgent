package com.myagent.run.application.result;

/**
 * 运行详情中的 Agent 摘要。
 *
 * @param agentId Agent 主键
 * @param agentKey Agent 业务标识
 * @param agentName Agent 名称
 */
public record RunAgentResult(long agentId, String agentKey, String agentName) {
}
