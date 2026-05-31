package com.myagent.runtime;

import java.time.Instant;

/**
 * 节点运行开始结果。
 *
 * @param nodeRunDbId NodeRun 数据库主键
 * @param agentRunDbId AgentRun 数据库主键
 * @param agentRunNo AgentRun 对外编号
 * @param nodeId 节点标识
 * @param startedAt 开始时间
 */
public record NodeRunStartResult(
        long nodeRunDbId,
        long agentRunDbId,
        String agentRunNo,
        String nodeId,
        Instant startedAt
) {
}
