package com.myagent.agent.repository;

import com.myagent.agent.application.query.ListAgentsQuery;
import com.myagent.common.domain.EnableStatus;
import com.myagent.common.page.PageResult;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 基于 MyBatis 的 Agent 仓储实现。
 */
@Repository
public class MyBatisAgentRepository implements AgentRepository {

    /**
     * Agent Mapper。
     */
    private final AgentMapper agentMapper;

    /**
     * 构造 Agent 仓储。
     *
     * @param agentMapper Agent Mapper
     */
    public MyBatisAgentRepository(AgentMapper agentMapper) {
        this.agentMapper = agentMapper;
    }

    /**
     * 分页查询 Agent。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    @Override
    public PageResult<AgentRecord> listAgents(ListAgentsQuery query) {
        long offset = (query.page() - 1L) * query.pageSize();
        return PageResult.of(
                agentMapper.listAgents(query.keyword(), query.status(), query.pageSize(), offset),
                query.page(),
                query.pageSize(),
                agentMapper.countAgents(query.keyword(), query.status())
        );
    }

    /**
     * 按主键查询 Agent。
     *
     * @param agentId Agent 主键
     * @return Agent 记录
     */
    @Override
    public Optional<AgentRecord> findById(long agentId) {
        return Optional.ofNullable(agentMapper.findById(agentId));
    }

    /**
     * 按业务标识查询 Agent。
     *
     * @param agentKey Agent 业务标识
     * @return Agent 记录
     */
    @Override
    public Optional<AgentRecord> findByAgentKey(String agentKey) {
        return Optional.ofNullable(agentMapper.findByAgentKey(agentKey));
    }

    /**
     * 插入 Agent。
     *
     * @param record Agent 记录
     * @return 新增后的记录
     */
    @Override
    public AgentRecord insert(AgentRecord record) {
        agentMapper.insert(record);
        return findByAgentKey(record.agentKey()).orElseThrow();
    }

    /**
     * 更新 Agent 基础信息。
     *
     * @param record Agent 记录
     * @return 受影响行数
     */
    @Override
    public int update(AgentRecord record) {
        return agentMapper.update(record);
    }

    /**
     * 更新 Agent 状态。
     *
     * @param agentId Agent 主键
     * @param status 状态
     * @return 受影响行数
     */
    @Override
    public int updateStatus(long agentId, EnableStatus status) {
        return agentMapper.updateStatus(agentId, status);
    }

    /**
     * 更新当前草稿版本指针。
     *
     * @param agentId Agent 主键
     * @param workflowVersionId 工作流版本主键
     * @return 受影响行数
     */
    @Override
    public int updateCurrentDraftWorkflowVersionId(long agentId, Long workflowVersionId) {
        return agentMapper.updateCurrentDraftWorkflowVersionId(agentId, workflowVersionId);
    }

    /**
     * 更新当前发布版本指针。
     *
     * @param agentId Agent 主键
     * @param workflowVersionId 工作流版本主键
     * @return 受影响行数
     */
    @Override
    public int updateCurrentPublishedWorkflowVersionId(long agentId, Long workflowVersionId) {
        return agentMapper.updateCurrentPublishedWorkflowVersionId(agentId, workflowVersionId);
    }
}
