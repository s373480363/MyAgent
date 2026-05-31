package com.myagent.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.myagent.agent.repository.AgentRecord;

/**
 * 工作流运行引擎。
 */
public interface WorkflowRuntimeEngine {

    /**
     * 执行工作流版本快照。
     *
     * @param agentRunDbId AgentRun 数据库主键
     * @param agentRunNo AgentRun 对外编号
     * @param agent Agent 主数据
     * @param snapshot 工作流版本快照
     * @param input 输入 JSON
     * @return 执行结果
     */
    WorkflowRuntimeResult execute(
            long agentRunDbId,
            String agentRunNo,
            AgentRecord agent,
            WorkflowVersionSnapshot snapshot,
            JsonNode input
    );
}
