package com.myagent.eval.application.result;

/**
 * 验收关联 Agent 摘要。
 *
 * @param agentId Agent 主键
 * @param agentKey Agent 业务标识
 * @param agentName Agent 名称
 */
public record EvalAgentSummaryResult(long agentId, String agentKey, String agentName) {
}
