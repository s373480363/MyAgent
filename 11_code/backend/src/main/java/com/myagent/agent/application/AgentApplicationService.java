package com.myagent.agent.application;

import com.myagent.agent.application.command.ChangeAgentStatusCommand;
import com.myagent.agent.application.command.CreateAgentCommand;
import com.myagent.agent.application.command.UpdateAgentCommand;
import com.myagent.agent.application.query.GetAgentDetailQuery;
import com.myagent.agent.application.query.ListAgentsQuery;
import com.myagent.agent.application.result.AgentDetailResult;
import com.myagent.agent.application.result.AgentListItemResult;
import com.myagent.common.page.PageResult;

/**
 * Agent 应用服务接口。
 */
public interface AgentApplicationService {

    /**
     * 创建 Agent。
     *
     * @param command 创建命令
     * @return Agent 详情
     */
    AgentDetailResult createAgent(CreateAgentCommand command);

    /**
     * 更新 Agent。
     *
     * @param command 更新命令
     * @return Agent 详情
     */
    AgentDetailResult updateAgent(UpdateAgentCommand command);

    /**
     * 修改 Agent 状态。
     *
     * @param command 状态修改命令
     */
    void changeAgentStatus(ChangeAgentStatusCommand command);

    /**
     * 分页查询 Agent 列表。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    PageResult<AgentListItemResult> listAgents(ListAgentsQuery query);

    /**
     * 查询 Agent 详情。
     *
     * @param query 查询条件
     * @return Agent 详情
     */
    AgentDetailResult getAgentDetail(GetAgentDetailQuery query);
}
