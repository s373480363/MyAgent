package com.myagent.workflow.repository;

import com.myagent.common.page.PageResult;
import com.myagent.workflow.application.query.ListWorkflowVersionsQuery;
import com.myagent.workflow.domain.WorkflowVersionStatus;

import java.util.Optional;

/**
 * 工作流版本仓储接口。
 */
public interface WorkflowVersionRepository {

    /**
     * 按主键查询工作流版本。
     *
     * @param workflowVersionId 工作流版本主键
     * @return 版本记录
     */
    Optional<WorkflowVersionRecord> findById(long workflowVersionId);

    /**
     * 查询当前草稿版本。
     *
     * @param agentId Agent 主键
     * @return 版本记录
     */
    Optional<WorkflowVersionRecord> findCurrentDraft(long agentId);

    /**
     * 查询当前发布版本。
     *
     * @param agentId Agent 主键
     * @return 版本记录
     */
    Optional<WorkflowVersionRecord> findCurrentPublished(long agentId);

    /**
     * 查询当前 Agent 的最大版本号。
     *
     * @param agentId Agent 主键
     * @return 最大版本号，不存在时返回 0
     */
    int findMaxVersionNo(long agentId);

    /**
     * 插入工作流版本。
     *
     * @param record 版本记录
     * @return 新增后的记录
     */
    WorkflowVersionRecord insert(WorkflowVersionRecord record);

    /**
     * 更新工作流版本状态。
     *
     * @param workflowVersionId 工作流版本主键
     * @param status 新状态
     * @return 受影响行数
     */
    int updateStatus(long workflowVersionId, WorkflowVersionStatus status);

    /**
     * 分页查询工作流版本列表。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    PageResult<WorkflowVersionRecord> listWorkflowVersions(ListWorkflowVersionsQuery query);

    /**
     * 汇总历史版本入口摘要。
     *
     * @param agentId Agent 主键
     * @return 历史摘要
     */
    WorkflowVersionHistorySummaryRecord summarizeHistory(long agentId);
}
