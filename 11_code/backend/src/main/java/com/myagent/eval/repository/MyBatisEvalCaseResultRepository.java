package com.myagent.eval.repository;

import com.myagent.common.page.PageResult;
import com.myagent.eval.application.query.ListEvalRunResultsQuery;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 基于 MyBatis 的 EvalCaseResult 仓储实现。
 */
@Repository
public class MyBatisEvalCaseResultRepository implements EvalCaseResultRepository {

    /**
     * EvalCaseResult Mapper。
     */
    private final EvalCaseResultMapper mapper;

    /**
     * 构造仓储。
     *
     * @param mapper EvalCaseResult Mapper
     */
    public MyBatisEvalCaseResultRepository(EvalCaseResultMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public EvalCaseResultRecord insert(EvalCaseResultRecord record) {
        return mapper.insert(record);
    }

    @Override
    public PageResult<EvalCaseResultJoinedRecord> listByEvalRun(long evalRunDbId, ListEvalRunResultsQuery query) {
        long offset = (query.page() - 1L) * query.pageSize();
        return PageResult.of(
                mapper.listByEvalRun(evalRunDbId, query.passed(), query.critical(), query.keyword(), query.pageSize(), offset),
                query.page(),
                query.pageSize(),
                mapper.countByEvalRun(evalRunDbId, query.passed(), query.critical(), query.keyword())
        );
    }

    @Override
    public List<EvalCaseResultJoinedRecord> listFailed(long evalRunDbId) {
        return mapper.listFailed(evalRunDbId);
    }

    @Override
    public long countCriticalFailures(long evalRunDbId) {
        return mapper.countCriticalFailures(evalRunDbId);
    }
}
