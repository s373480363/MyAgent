package com.myagent.run.repository;

import java.util.List;

/**
 * Agent 消息仓储。
 */
public interface AgentMessageRepository {

    /**
     * 插入 Agent 消息。
     *
     * @param record Agent 消息
     * @return 新增后的记录
     */
    AgentMessageRecord insert(AgentMessageRecord record);

    /**
     * 查询父运行下的消息。
     *
     * @param parentRunId 父运行主键
     * @return 消息列表
     */
    List<AgentMessageRecord> listByParentRunId(long parentRunId);
}
