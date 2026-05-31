package com.myagent.run.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.myagent.common.page.PageResult;
import com.myagent.run.application.query.ListRunsQuery;
import com.myagent.run.domain.RunStatus;

import java.util.Optional;

/**
 * AgentRun 仓储。
 */
public interface AgentRunRepository {

    /**
     * 插入运行记录。
     *
     * @param record 运行记录
     * @return 新增后的记录
     */
    AgentRunRecord insert(AgentRunRecord record);

    /**
     * 按运行编号查询。
     *
     * @param runNo 运行编号
     * @return 运行记录
     */
    Optional<AgentRunRecord> findByRunNo(String runNo);

    /**
     * 按主键查询。
     *
     * @param runId 运行主键
     * @return 运行记录
     */
    Optional<AgentRunRecord> findById(long runId);

    /**
     * 分页查询运行记录。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    PageResult<AgentRunRecord> listRuns(ListRunsQuery query);

    /**
     * 更新运行状态。
     *
     * @param runId 运行主键
     * @param status 运行状态
     * @param outputJson 输出 JSON
     * @param errorMessage 错误消息
     * @param durationMs 耗时毫秒
     * @return 受影响行数
     */
    int finishRun(long runId, RunStatus status, JsonNode outputJson, String errorMessage, long durationMs);

    /**
     * 更新运行状态为运行中。
     *
     * @param runId 运行主键
     * @return 受影响行数
     */
    int markRunning(long runId);
}
