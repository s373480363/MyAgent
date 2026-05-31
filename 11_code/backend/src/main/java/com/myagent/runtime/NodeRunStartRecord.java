package com.myagent.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.myagent.workflow.domain.WorkflowNodeType;

/**
 * 节点运行开始记录。
 *
 * @param agentRunDbId AgentRun 数据库主键
 * @param agentRunNo AgentRun 对外编号
 * @param nodeId 节点标识
 * @param nodeName 节点名称
 * @param nodeType 节点类型
 * @param inputJson 节点输入
 */
public record NodeRunStartRecord(
        long agentRunDbId,
        String agentRunNo,
        String nodeId,
        String nodeName,
        WorkflowNodeType nodeType,
        JsonNode inputJson
) {
}
