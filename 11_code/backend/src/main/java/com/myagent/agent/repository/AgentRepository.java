package com.myagent.agent.repository;

import com.myagent.common.domain.EnableStatus;
import com.myagent.common.page.PageResult;
import com.myagent.agent.application.query.ListAgentsQuery;

import java.util.Optional;

/**
 * Agent 仓储接口。
 */
public interface AgentRepository {

    /**
     * 分页查询 Agent。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    PageResult<AgentRecord> listAgents(ListAgentsQuery query);

    /**
     * 按主键查询 Agent。
     *
     * @param agentId Agent 主键
     * @return Agent 记录
     */
    Optional<AgentRecord> findById(long agentId);

    /**
     * 按业务标识查询 Agent。
     *
     * @param agentKey Agent 业务标识
     * @return Agent 记录
     */
    Optional<AgentRecord> findByAgentKey(String agentKey);

    /**
     * 插入 Agent。
     *
     * @param record Agent 记录
     * @return 新增后的记录
     */
    AgentRecord insert(AgentRecord record);

    /**
     * 更新 Agent 基础信息。
     *
     * @param record Agent 记录
     * @return 受影响行数
     */
    int update(AgentRecord record);

    /**
     * 更新 Agent 状态。
     *
     * @param agentId Agent 主键
     * @param status 状态
     * @return 受影响行数
     */
    int updateStatus(long agentId, EnableStatus status);

    /**
     * 更新当前草稿版本指针。
     *
     * @param agentId Agent 主键
     * @param workflowVersionId 工作流版本主键
     * @return 受影响行数
     */
    int updateCurrentDraftWorkflowVersionId(long agentId, Long workflowVersionId);

    /**
     * 更新当前发布版本指针。
     *
     * @param agentId Agent 主键
     * @param workflowVersionId 工作流版本主键
     * @return 受影响行数
     */
    int updateCurrentPublishedWorkflowVersionId(long agentId, Long workflowVersionId);
}
