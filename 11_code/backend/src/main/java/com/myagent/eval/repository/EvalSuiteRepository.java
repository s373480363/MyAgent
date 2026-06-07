package com.myagent.eval.repository;

import com.myagent.common.page.PageResult;
import com.myagent.eval.application.query.ListEvalSuitesQuery;
import com.myagent.eval.domain.EvalSuiteStatus;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * EvalSuite 仓储。
 */
public interface EvalSuiteRepository {

    /**
     * 新增验收套件。
     *
     * @param record 套件记录
     * @return 新增后的套件
     */
    EvalSuiteRecord insert(EvalSuiteRecord record);

    /**
     * 按主键查询。
     *
     * @param suiteId 套件主键
     * @return 套件记录
     */
    Optional<EvalSuiteRecord> findById(long suiteId);

    /**
     * 分页查询。
     *
     * @param query 查询参数
     * @return 分页结果
     */
    PageResult<EvalSuiteRecord> list(ListEvalSuitesQuery query);

    /**
     * 更新套件。
     *
     * @param suiteId 套件主键
     * @param name 名称
     * @param goal 目标
     * @param passThreshold 通过阈值
     * @return 更新后的套件
     */
    EvalSuiteRecord update(
            long suiteId,
            String name,
            String goal,
            String judgeModelOfferingKey,
            BigDecimal judgeTemperature,
            BigDecimal passThreshold
    );

    /**
     * 更新状态。
     *
     * @param suiteId 套件主键
     * @param status 目标状态
     * @return 更新后的套件
     */
    EvalSuiteRecord updateStatus(long suiteId, EvalSuiteStatus status);
}
