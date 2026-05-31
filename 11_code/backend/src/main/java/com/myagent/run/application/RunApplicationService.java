package com.myagent.run.application;

import com.myagent.common.page.PageResult;
import com.myagent.run.application.command.RunDebugAgentCommand;
import com.myagent.run.application.command.RunPublishedAgentCommand;
import com.myagent.run.application.query.GetRunDetailQuery;
import com.myagent.run.application.query.ListRunsQuery;
import com.myagent.run.application.result.RunDetailResult;
import com.myagent.run.application.result.RunListItemResult;
import com.myagent.run.application.result.RunResult;

/**
 * 运行应用服务。
 */
public interface RunApplicationService {

    /**
     * 运行当前发布版本 Agent。
     *
     * @param command 正式运行命令
     * @return 运行结果
     */
    RunResult runPublishedAgent(RunPublishedAgentCommand command);

    /**
     * 调试运行 Agent。
     *
     * @param command 调试运行命令
     * @return 运行结果
     */
    RunResult runDebugAgent(RunDebugAgentCommand command);

    /**
     * 查询运行列表。
     *
     * @param query 查询条件
     * @return 分页运行列表
     */
    PageResult<RunListItemResult> listRuns(ListRunsQuery query);

    /**
     * 查询运行详情。
     *
     * @param query 查询条件
     * @return 运行详情
     */
    RunDetailResult getRunDetail(GetRunDetailQuery query);
}
