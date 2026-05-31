package com.myagent.eval.repository;

import com.myagent.common.page.PageResult;
import com.myagent.eval.application.query.ListEvalCasesQuery;
import com.myagent.eval.domain.EvalCaseConfirmStatus;

import java.util.List;
import java.util.Optional;

/**
 * EvalCase 仓储。
 */
public interface EvalCaseRepository {

    /**
     * 新增用例。
     *
     * @param record 用例记录
     * @return 新增后的用例
     */
    EvalCaseRecord insert(EvalCaseRecord record);

    /**
     * 按主键查询。
     *
     * @param caseId 用例主键
     * @return 用例记录
     */
    Optional<EvalCaseRecord> findById(long caseId);

    /**
     * 查询套件下用例。
     *
     * @param query 查询参数
     * @return 分页结果
     */
    PageResult<EvalCaseRecord> list(ListEvalCasesQuery query);

    /**
     * 查询可执行用例。
     *
     * @param suiteId 套件主键
     * @param caseIds 指定用例主键
     * @param includeUnconfirmed 是否包含未确认用例
     * @return 用例列表
     */
    List<EvalCaseRecord> listRunnableCases(long suiteId, List<Long> caseIds, boolean includeUnconfirmed);

    /**
     * 统计正式用例数。
     *
     * @param suiteId 套件主键
     * @return 正式用例数
     */
    long countFormalCases(long suiteId);

    /**
     * 更新用例。
     *
     * @param record 用例记录
     * @return 更新后的用例
     */
    EvalCaseRecord update(EvalCaseRecord record);

    /**
     * 更新确认状态。
     *
     * @param suiteId 套件主键
     * @param caseId 用例主键
     * @param status 目标状态
     * @return 更新后的用例
     */
    EvalCaseRecord updateConfirmStatus(long suiteId, long caseId, EvalCaseConfirmStatus status);
}
