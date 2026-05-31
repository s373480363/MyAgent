package com.myagent.eval.repository;

import com.myagent.common.page.PageResult;
import com.myagent.eval.application.query.ListEvalCasesQuery;
import com.myagent.eval.domain.EvalCaseConfirmStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 基于 MyBatis 的 EvalCase 仓储实现。
 */
@Repository
public class MyBatisEvalCaseRepository implements EvalCaseRepository {

    /**
     * EvalCase Mapper。
     */
    private final EvalCaseMapper mapper;

    /**
     * 构造仓储。
     *
     * @param mapper EvalCase Mapper
     */
    public MyBatisEvalCaseRepository(EvalCaseMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public EvalCaseRecord insert(EvalCaseRecord record) {
        return mapper.insert(record);
    }

    @Override
    public Optional<EvalCaseRecord> findById(long caseId) {
        return Optional.ofNullable(mapper.findById(caseId));
    }

    @Override
    public PageResult<EvalCaseRecord> list(ListEvalCasesQuery query) {
        long offset = (query.page() - 1L) * query.pageSize();
        return PageResult.of(
                mapper.list(query.suiteId(), query.confirmStatus(), query.critical(), query.keyword(), query.pageSize(), offset),
                query.page(),
                query.pageSize(),
                mapper.count(query.suiteId(), query.confirmStatus(), query.critical(), query.keyword())
        );
    }

    @Override
    public List<EvalCaseRecord> listRunnableCases(long suiteId, List<Long> caseIds, boolean includeUnconfirmed) {
        return mapper.listRunnableCases(suiteId, caseIds == null || caseIds.isEmpty() ? null : caseIds, includeUnconfirmed);
    }

    @Override
    public long countFormalCases(long suiteId) {
        return mapper.countFormalCases(suiteId);
    }

    @Override
    public EvalCaseRecord update(EvalCaseRecord record) {
        return mapper.update(record);
    }

    @Override
    public EvalCaseRecord updateConfirmStatus(long suiteId, long caseId, EvalCaseConfirmStatus status) {
        return mapper.updateConfirmStatus(suiteId, caseId, status);
    }
}
