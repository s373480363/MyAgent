package com.myagent.eval.repository;

import com.myagent.common.page.PageResult;
import com.myagent.eval.application.query.ListEvalSuitesQuery;
import com.myagent.eval.domain.EvalSuiteStatus;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * 基于 MyBatis 的 EvalSuite 仓储实现。
 */
@Repository
public class MyBatisEvalSuiteRepository implements EvalSuiteRepository {

    /**
     * EvalSuite Mapper。
     */
    private final EvalSuiteMapper mapper;

    /**
     * 构造仓储。
     *
     * @param mapper EvalSuite Mapper
     */
    public MyBatisEvalSuiteRepository(EvalSuiteMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public EvalSuiteRecord insert(EvalSuiteRecord record) {
        return mapper.insert(record);
    }

    @Override
    public Optional<EvalSuiteRecord> findById(long suiteId) {
        return Optional.ofNullable(mapper.findById(suiteId));
    }

    @Override
    public PageResult<EvalSuiteRecord> list(ListEvalSuitesQuery query) {
        long offset = (query.page() - 1L) * query.pageSize();
        return PageResult.of(
                mapper.list(query.agentId(), query.workflowVersionId(), query.nodeId(), query.status(),
                        query.keyword(), query.pageSize(), offset),
                query.page(),
                query.pageSize(),
                mapper.count(query.agentId(), query.workflowVersionId(), query.nodeId(), query.status(), query.keyword())
        );
    }

    @Override
    public EvalSuiteRecord update(
            long suiteId,
            String name,
            String goal,
            String judgeModelOfferingKey,
            BigDecimal judgeTemperature,
            BigDecimal passThreshold
    ) {
        return mapper.update(
                suiteId,
                name,
                goal == null ? "" : goal,
                judgeModelOfferingKey,
                judgeTemperature,
                passThreshold
        );
    }

    @Override
    public EvalSuiteRecord updateStatus(long suiteId, EvalSuiteStatus status) {
        return mapper.updateStatus(suiteId, status);
    }
}
