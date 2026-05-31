package com.myagent.eval.repository;

import com.myagent.common.page.PageResult;
import com.myagent.eval.application.query.ListEvalRunsQuery;
import com.myagent.run.domain.RunStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * EvalRun 仓储。
 */
public interface EvalRunRepository {

    /**
     * 新增验收运行。
     *
     * @param record 验收运行记录
     * @return 新增后的记录
     */
    EvalRunRecord insert(EvalRunRecord record);

    /**
     * 按对外编号查询。
     *
     * @param runNo 对外验收运行编号
     * @return 验收运行
     */
    Optional<EvalRunRecord> findByRunNo(String runNo);

    /**
     * 按关联 AgentRun 查询。
     *
     * @param agentRunId AgentRun 数据库主键
     * @return 验收运行
     */
    Optional<EvalRunRecord> findByAgentRunId(long agentRunId);

    /**
     * 查询套件运行列表。
     *
     * @param query 查询参数
     * @return 分页结果
     */
    PageResult<EvalRunRecord> listBySuite(ListEvalRunsQuery query);

    /**
     * 查询套件历史运行。
     *
     * @param suiteId 套件主键
     * @param page 页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    PageResult<EvalRunRecord> listHistory(long suiteId, long page, long pageSize);

    /**
     * 查询指定运行之前的上一轮运行。
     *
     * @param suiteId 套件主键
     * @param evalRunDbId 当前 EvalRun 主键
     * @return 上一轮运行
     */
    Optional<EvalRunRecord> findPrevious(long suiteId, long evalRunDbId);

    /**
     * 完成验收运行。
     *
     * @param evalRunId EvalRun 数据库主键
     * @param status 状态
     * @param total 总数
     * @param passed 通过数
     * @param failed 失败数
     * @param passRate 通过率
     * @param summary 摘要
     * @param errorMessage 错误消息
     * @param durationMs 耗时毫秒
     * @return 更新后的记录
     */
    EvalRunRecord finish(long evalRunId, RunStatus status, int total, int passed, int failed,
                         BigDecimal passRate, String summary, String errorMessage, long durationMs);
}
