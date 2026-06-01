package com.myagent.eval.application;

import com.myagent.common.page.PageResult;
import com.myagent.eval.application.command.CreateEvalCaseCommand;
import com.myagent.eval.application.command.CreateEvalCaseFromNodeRunCommand;
import com.myagent.eval.application.command.UpdateEvalCaseCommand;
import com.myagent.eval.application.query.ListEvalCasesQuery;
import com.myagent.eval.application.result.EvalCaseResult;

/**
 * 验收用例应用服务。
 */
public interface EvalCaseApplicationService {

    /**
     * 查询套件用例列表。
     *
     * @param query 查询参数
     * @return 用例分页
     */
    PageResult<EvalCaseResult> listCases(ListEvalCasesQuery query);

    /**
     * 创建验收用例。
     *
     * @param command 创建命令
     * @return 用例详情
     */
    EvalCaseResult createCase(CreateEvalCaseCommand command);

    /**
     * 从 NodeRun 创建验收用例。
     *
     * @param command 创建命令
     * @return 用例详情
     */
    EvalCaseResult createCaseFromNodeRun(CreateEvalCaseFromNodeRunCommand command);

    /**
     * 查询用例详情。
     *
     * @param suiteId 套件主键
     * @param caseId 用例主键
     * @return 用例详情
     */
    EvalCaseResult getCase(long suiteId, long caseId);

    /**
     * 更新验收用例。
     *
     * @param command 更新命令
     * @return 用例详情
     */
    EvalCaseResult updateCase(UpdateEvalCaseCommand command);

    /**
     * 确认验收用例。
     *
     * @param suiteId 套件主键
     * @param caseId 用例主键
     * @return 用例详情
     */
    EvalCaseResult confirmCase(long suiteId, long caseId);

    /**
     * 归档验收用例。
     *
     * @param suiteId 套件主键
     * @param caseId 用例主键
     * @return 用例详情
     */
    EvalCaseResult archiveCase(long suiteId, long caseId);
}
