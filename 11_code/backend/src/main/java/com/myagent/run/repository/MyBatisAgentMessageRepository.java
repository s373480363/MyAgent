package com.myagent.run.repository;

import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 基于 MyBatis 的 AgentMessage 仓储实现。
 */
@Repository
public class MyBatisAgentMessageRepository implements AgentMessageRepository {

    /**
     * AgentMessage Mapper。
     */
    private final AgentMessageMapper agentMessageMapper;

    /**
     * 构造 AgentMessage 仓储。
     *
     * @param agentMessageMapper AgentMessage Mapper
     */
    public MyBatisAgentMessageRepository(AgentMessageMapper agentMessageMapper) {
        this.agentMessageMapper = agentMessageMapper;
    }

    /**
     * 插入 Agent 消息。
     *
     * @param record Agent 消息
     * @return 新增后的记录
     */
    @Override
    public AgentMessageRecord insert(AgentMessageRecord record) {
        agentMessageMapper.insert(record);
        return agentMessageMapper.findByChildRunId(record.childRunId());
    }

    /**
     * 查询父运行下的消息。
     *
     * @param parentRunId 父运行主键
     * @return 消息列表
     */
    @Override
    public List<AgentMessageRecord> listByParentRunId(long parentRunId) {
        return agentMessageMapper.listByParentRunId(parentRunId);
    }
}
