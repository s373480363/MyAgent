package com.myagent.eval.application;

import com.myagent.common.page.PageResult;
import com.myagent.eval.application.command.CreateEvalCaseCommand;
import com.myagent.eval.application.command.CreateEvalCaseFromNodeRunCommand;
import com.myagent.eval.application.command.CreateEvalSuiteCommand;
import com.myagent.eval.application.command.RunEvalSuiteCommand;
import com.myagent.eval.application.command.UpdateEvalCaseCommand;
import com.myagent.eval.application.command.UpdateEvalSuiteCommand;
import com.myagent.eval.application.query.GetEvalRunQuery;
import com.myagent.eval.application.query.ListEvalCasesQuery;
import com.myagent.eval.application.query.ListEvalRunHistoryQuery;
import com.myagent.eval.application.query.ListEvalRunResultsQuery;
import com.myagent.eval.application.query.ListEvalRunsQuery;
import com.myagent.eval.application.query.ListEvalSuitesQuery;
import com.myagent.eval.application.result.EvalCaseResult;
import com.myagent.eval.application.result.EvalRunDetailResult;
import com.myagent.eval.application.result.EvalRunHistoryItemResult;
import com.myagent.eval.application.result.EvalRunListItemResult;
import com.myagent.eval.application.result.EvalRunResult;
import com.myagent.eval.application.result.EvalRunResultItemResult;
import com.myagent.eval.application.result.EvalSuiteListItemResult;
import com.myagent.eval.application.result.EvalSuiteResult;

/**
 * 节点验收应用服务。
 */
public interface EvalApplicationService {

    /**
     * 查询验收套件列表。
     *
     * @param query 查询参数
     * @return 分页结果
     */
    PageResult<EvalSuiteListItemResult> listSuites(ListEvalSuitesQuery query);

    /**
     * 创建验收套件。
     *
     * @param command 创建命令
     * @return 套件详情
     */
    EvalSuiteResult createSuite(CreateEvalSuiteCommand command);

    /**
     * 更新验收套件。
     *
     * @param command 更新命令
     * @return 套件详情
     */
    EvalSuiteResult updateSuite(UpdateEvalSuiteCommand command);

    /**
     * 确认验收套件。
     *
     * @param suiteId 套件主键
     * @return 套件详情
     */
    EvalSuiteResult confirmSuite(long suiteId);

    /**
     * 归档验收套件。
     *
     * @param suiteId 套件主键
     * @return 套件详情
     */
    EvalSuiteResult archiveSuite(long suiteId);

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
     * 更新用例。
     *
     * @param command 更新命令
     * @return 用例详情
     */
    EvalCaseResult updateCase(UpdateEvalCaseCommand command);

    /**
     * 确认用例。
     *
     * @param suiteId 套件主键
     * @param caseId 用例主键
     * @return 用例详情
     */
    EvalCaseResult confirmCase(long suiteId, long caseId);

    /**
     * 归档用例。
     *
     * @param suiteId 套件主键
     * @param caseId 用例主键
     * @return 用例详情
     */
    EvalCaseResult archiveCase(long suiteId, long caseId);

    /**
     * 运行验收套件。
     *
     * @param command 运行命令
     * @return 验收运行结果
     */
    EvalRunResult runSuite(RunEvalSuiteCommand command);

    /**
     * 查询验收运行列表。
     *
     * @param query 查询参数
     * @return 分页结果
     */
    PageResult<EvalRunListItemResult> listRuns(ListEvalRunsQuery query);

    /**
     * 查询验收运行详情。
     *
     * @param query 查询参数
     * @return 运行详情
     */
    EvalRunDetailResult getRun(GetEvalRunQuery query);

    /**
     * 查询验收结果明细。
     *
     * @param query 查询参数
     * @return 结果明细分页
     */
    PageResult<EvalRunResultItemResult> listRunResults(ListEvalRunResultsQuery query);

    /**
     * 查询验收历史对比。
     *
     * @param query 查询参数
     * @return 历史对比分页
     */
    PageResult<EvalRunHistoryItemResult> listRunHistory(ListEvalRunHistoryQuery query);
}
