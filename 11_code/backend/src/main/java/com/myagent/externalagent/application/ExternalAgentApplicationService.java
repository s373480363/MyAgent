package com.myagent.externalagent.application;

import com.myagent.common.page.PageResult;
import com.myagent.externalagent.application.command.ChangeExternalAgentStatusCommand;
import com.myagent.externalagent.application.command.CreateExternalAgentCommand;
import com.myagent.externalagent.application.command.TestExternalAgentCommand;
import com.myagent.externalagent.application.command.UpdateExternalAgentCommand;
import com.myagent.externalagent.application.command.UpdateExternalAgentSecretsCommand;
import com.myagent.externalagent.application.query.GetExternalAgentQuery;
import com.myagent.externalagent.application.query.ListExternalAgentsQuery;
import com.myagent.externalagent.application.result.ExternalAgentDetailResult;
import com.myagent.externalagent.application.result.ExternalAgentListItemResult;
import com.myagent.externalagent.application.result.ExternalAgentTestResult;

/**
 * 外部 Agent 应用服务。
 */
public interface ExternalAgentApplicationService {

    /**
     * 分页查询外部 Agent。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    PageResult<ExternalAgentListItemResult> listExternalAgents(ListExternalAgentsQuery query);

    /**
     * 查询外部 Agent 详情。
     *
     * @param query 查询条件
     * @return 详情结果
     */
    ExternalAgentDetailResult getExternalAgent(GetExternalAgentQuery query);

    /**
     * 创建外部 Agent。
     *
     * @param command 创建命令
     * @return 创建后的详情
     */
    ExternalAgentDetailResult createExternalAgent(CreateExternalAgentCommand command);

    /**
     * 更新外部 Agent。
     *
     * @param command 更新命令
     * @return 更新后的详情
     */
    ExternalAgentDetailResult updateExternalAgent(UpdateExternalAgentCommand command);

    /**
     * 单独更新敏感 secret。
     *
     * @param command 更新命令
     * @return 更新后的详情
     */
    ExternalAgentDetailResult updateExternalAgentSecrets(UpdateExternalAgentSecretsCommand command);

    /**
     * 更新外部 Agent 状态。
     *
     * @param command 更新命令
     */
    void changeExternalAgentStatus(ChangeExternalAgentStatusCommand command);

    /**
     * 测试外部 Agent。
     *
     * @param command 测试命令
     * @return 测试结果
     */
    ExternalAgentTestResult testExternalAgent(TestExternalAgentCommand command);

    /**
     * 刷新外部 Agent 目录。
     */
    void refreshExternalAgentCatalog();
}
