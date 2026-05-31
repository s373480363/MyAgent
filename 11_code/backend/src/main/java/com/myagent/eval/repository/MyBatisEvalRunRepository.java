package com.myagent.eval.repository;

import com.myagent.common.page.PageResult;
import com.myagent.eval.application.query.ListEvalRunsQuery;
import com.myagent.run.domain.RunStatus;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * 基于 MyBatis 的 EvalRun 仓储实现。
 */
@Repository
public class MyBatisEvalRunRepository implements EvalRunRepository {

    /**
     * EvalRun Mapper。
     */
    private final EvalRunMapper mapper;

    /**
     * 构造仓储。
     *
     * @param mapper EvalRun Mapper
     */
    public MyBatisEvalRunRepository(EvalRunMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public EvalRunRecord insert(EvalRunRecord record) {
        return mapper.insert(record);
    }

    @Override
    public Optional<EvalRunRecord> findByRunNo(String runNo) {
        return Optional.ofNullable(mapper.findByRunNo(runNo));
    }

    @Override
    public Optional<EvalRunRecord> findByAgentRunId(long agentRunId) {
        return Optional.ofNullable(mapper.findByAgentRunId(agentRunId));
    }

    @Override
    public PageResult<EvalRunRecord> listBySuite(ListEvalRunsQuery query) {
        long offset = (query.page() - 1L) * query.pageSize();
        return PageResult.of(
                mapper.listBySuite(query.suiteId(), query.status(), query.startedAtFrom(),
                        query.startedAtTo(), query.pageSize(), offset),
                query.page(),
                query.pageSize(),
                mapper.countBySuite(query.suiteId(), query.status(), query.startedAtFrom(), query.startedAtTo())
        );
    }

    @Override
    public PageResult<EvalRunRecord> listHistory(long suiteId, long page, long pageSize) {
        long offset = (page - 1L) * pageSize;
        return PageResult.of(mapper.listHistory(suiteId, pageSize, offset), page, pageSize, mapper.countHistory(suiteId));
    }

    @Override
    public Optional<EvalRunRecord> findPrevious(long suiteId, long evalRunDbId) {
        return Optional.ofNullable(mapper.findPrevious(suiteId, evalRunDbId));
    }

    @Override
    public EvalRunRecord finish(long evalRunId, RunStatus status, int total, int passed, int failed,
                                BigDecimal passRate, String summary, String errorMessage, long durationMs) {
        return mapper.finish(evalRunId, status, total, passed, failed, passRate, summary,
                errorMessage == null ? "" : errorMessage, durationMs);
    }
}
