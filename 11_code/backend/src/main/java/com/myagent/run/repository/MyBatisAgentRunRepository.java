package com.myagent.run.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.myagent.common.page.PageResult;
import com.myagent.run.application.query.ListRunsQuery;
import com.myagent.run.domain.RunStatus;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 基于 MyBatis 的 AgentRun 仓储实现。
 */
@Repository
public class MyBatisAgentRunRepository implements AgentRunRepository {

    /**
     * AgentRun Mapper。
     */
    private final AgentRunMapper agentRunMapper;

    /**
     * 构造 AgentRun 仓储。
     *
     * @param agentRunMapper AgentRun Mapper
     */
    public MyBatisAgentRunRepository(AgentRunMapper agentRunMapper) {
        this.agentRunMapper = agentRunMapper;
    }

    /**
     * 插入运行记录。
     *
     * @param record 运行记录
     * @return 新增后的记录
     */
    @Override
    public AgentRunRecord insert(AgentRunRecord record) {
        agentRunMapper.insert(record);
        return findByRunNo(record.runNo()).orElseThrow();
    }

    /**
     * 按运行编号查询。
     *
     * @param runNo 运行编号
     * @return 运行记录
     */
    @Override
    public Optional<AgentRunRecord> findByRunNo(String runNo) {
        return Optional.ofNullable(agentRunMapper.findByRunNo(runNo));
    }

    /**
     * 按主键查询。
     *
     * @param runId 运行主键
     * @return 运行记录
     */
    @Override
    public Optional<AgentRunRecord> findById(long runId) {
        return Optional.ofNullable(agentRunMapper.findById(runId));
    }

    /**
     * 分页查询运行记录。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    @Override
    public PageResult<AgentRunRecord> listRuns(ListRunsQuery query) {
        long offset = (query.page() - 1L) * query.pageSize();
        return PageResult.of(
                agentRunMapper.listRuns(
                        query.agentId(),
                        query.agentKey(),
                        query.runType(),
                        query.status(),
                        query.keyword(),
                        query.startedAtFrom(),
                        query.startedAtTo(),
                        query.pageSize(),
                        offset
                ),
                query.page(),
                query.pageSize(),
                agentRunMapper.countRuns(
                        query.agentId(),
                        query.agentKey(),
                        query.runType(),
                        query.status(),
                        query.keyword(),
                        query.startedAtFrom(),
                        query.startedAtTo()
                )
        );
    }

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
    @Override
    public int finishRun(long runId, RunStatus status, JsonNode outputJson, String errorMessage, long durationMs) {
        return agentRunMapper.finishRun(runId, status, outputJson, errorMessage == null ? "" : errorMessage, durationMs);
    }

    /**
     * 更新运行状态为运行中。
     *
     * @param runId 运行主键
     * @return 受影响行数
     */
    @Override
    public int markRunning(long runId) {
        return agentRunMapper.markRunning(runId);
    }
}
