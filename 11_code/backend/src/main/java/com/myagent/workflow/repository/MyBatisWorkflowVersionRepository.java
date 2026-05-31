package com.myagent.workflow.repository;

import com.myagent.common.page.PageResult;
import com.myagent.workflow.application.query.ListWorkflowVersionsQuery;
import com.myagent.workflow.domain.WorkflowVersionStatus;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 基于 MyBatis 的工作流版本仓储实现。
 */
@Repository
public class MyBatisWorkflowVersionRepository implements WorkflowVersionRepository {

    /**
     * 工作流版本 Mapper。
     */
    private final WorkflowVersionMapper workflowVersionMapper;

    /**
     * 构造工作流版本仓储。
     *
     * @param workflowVersionMapper 工作流版本 Mapper
     */
    public MyBatisWorkflowVersionRepository(WorkflowVersionMapper workflowVersionMapper) {
        this.workflowVersionMapper = workflowVersionMapper;
    }

    /**
     * 按主键查询工作流版本。
     *
     * @param workflowVersionId 工作流版本主键
     * @return 版本记录
     */
    @Override
    public Optional<WorkflowVersionRecord> findById(long workflowVersionId) {
        return Optional.ofNullable(workflowVersionMapper.findById(workflowVersionId));
    }

    /**
     * 查询当前草稿版本。
     *
     * @param agentId Agent 主键
     * @return 草稿版本
     */
    @Override
    public Optional<WorkflowVersionRecord> findCurrentDraft(long agentId) {
        return Optional.ofNullable(workflowVersionMapper.findCurrentDraft(agentId));
    }

    /**
     * 查询当前发布版本。
     *
     * @param agentId Agent 主键
     * @return 发布版本
     */
    @Override
    public Optional<WorkflowVersionRecord> findCurrentPublished(long agentId) {
        return Optional.ofNullable(workflowVersionMapper.findCurrentPublished(agentId));
    }

    /**
     * 查询最大版本号。
     *
     * @param agentId Agent 主键
     * @return 最大版本号
     */
    @Override
    public int findMaxVersionNo(long agentId) {
        Integer versionNo = workflowVersionMapper.findMaxVersionNo(agentId);
        return versionNo == null ? 0 : versionNo;
    }

    /**
     * 插入工作流版本。
     *
     * @param record 版本记录
     * @return 新增后的记录
     */
    @Override
    public WorkflowVersionRecord insert(WorkflowVersionRecord record) {
        workflowVersionMapper.insert(record);
        return Optional.ofNullable(workflowVersionMapper.findByAgentIdAndVersionNo(record.agentId(), record.versionNo()))
                .orElseThrow();
    }

    /**
     * 更新工作流版本状态。
     *
     * @param workflowVersionId 工作流版本主键
     * @param status 新状态
     * @return 受影响行数
     */
    @Override
    public int updateStatus(long workflowVersionId, WorkflowVersionStatus status) {
        return workflowVersionMapper.updateStatus(workflowVersionId, status);
    }

    /**
     * 分页查询工作流版本列表。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    @Override
    public PageResult<WorkflowVersionRecord> listWorkflowVersions(ListWorkflowVersionsQuery query) {
        long offset = (query.page() - 1L) * query.pageSize();
        return PageResult.of(
                workflowVersionMapper.listWorkflowVersions(query.agentId(), query.status(), query.pageSize(), offset),
                query.page(),
                query.pageSize(),
                workflowVersionMapper.countWorkflowVersions(query.agentId(), query.status())
        );
    }

    /**
     * 汇总历史版本入口摘要。
     *
     * @param agentId Agent 主键
     * @return 历史摘要
     */
    @Override
    public WorkflowVersionHistorySummaryRecord summarizeHistory(long agentId) {
        long total = workflowVersionMapper.countHistory(agentId);
        if (total == 0) {
            return new WorkflowVersionHistorySummaryRecord(0L, null, null, null);
        }
        WorkflowVersionRecord latest = workflowVersionMapper.findLatestHistory(agentId);
        return new WorkflowVersionHistorySummaryRecord(
                total,
                latest.id(),
                latest.versionNo(),
                latest.publishedAt()
        );
    }
}
