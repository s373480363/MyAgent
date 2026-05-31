package com.myagent.runtime;

import com.myagent.workflow.domain.WorkflowRuntimeOptions;

import java.time.Instant;

/**
 * 运行限制检查上下文。
 *
 * @param agentRunDbId AgentRun 数据库主键
 * @param agentRunNo AgentRun 对外编号
 * @param startedAt 运行开始时间
 * @param nodeStartedAt 节点开始时间
 * @param currentStep 当前执行步数
 * @param currentCallDepth 当前 Agent 调用深度
 * @param nodeTimeoutSeconds 节点超时秒数
 * @param runtimeOptions 工作流运行约束快照
 */
public record RunLimitContext(
        long agentRunDbId,
        String agentRunNo,
        Instant startedAt,
        Instant nodeStartedAt,
        int currentStep,
        int currentCallDepth,
        Integer nodeTimeoutSeconds,
        WorkflowRuntimeOptions runtimeOptions
) {
}
