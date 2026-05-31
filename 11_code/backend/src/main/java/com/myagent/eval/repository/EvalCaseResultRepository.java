package com.myagent.eval.repository;

import com.myagent.common.page.PageResult;
import com.myagent.eval.application.query.ListEvalRunResultsQuery;

import java.util.List;

/**
 * EvalCaseResult 仓储。
 */
public interface EvalCaseResultRepository {

    /**
     * 新增验收用例结果。
     *
     * @param record 用例结果记录
     * @return 新增后的记录
     */
    EvalCaseResultRecord insert(EvalCaseResultRecord record);

    /**
     * 查询验收运行结果明细。
     *
     * @param evalRunDbId EvalRun 数据库主键
     * @param query 查询条件
     * @return 分页结果
     */
    PageResult<EvalCaseResultJoinedRecord> listByEvalRun(long evalRunDbId, ListEvalRunResultsQuery query);

    /**
     * 查询失败摘要。
     *
     * @param evalRunDbId EvalRun 数据库主键
     * @return 失败结果列表
     */
    List<EvalCaseResultJoinedRecord> listFailed(long evalRunDbId);

    /**
     * 统计关键失败数。
     *
     * @param evalRunDbId EvalRun 数据库主键
     * @return 关键失败数
     */
    long countCriticalFailures(long evalRunDbId);
}
