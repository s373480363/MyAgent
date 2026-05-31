package com.myagent.eval.application.result;

/**
 * 验收关联节点摘要。
 *
 * @param nodeId 节点标识
 * @param nodeName 节点名称
 * @param nodeType 节点类型
 */
public record EvalNodeSummaryResult(String nodeId, String nodeName, String nodeType) {
}
