package com.myagent.workflow.validation;

import com.myagent.agent.repository.AgentRecord;
import com.myagent.workflow.application.result.WorkflowValidationResult;
import com.myagent.workflow.repository.WorkflowVersionRecord;

/**
 * 工作流草稿校验服务。
 */
public interface WorkflowDraftValidationService {

    /**
     * 校验当前草稿是否满足发布规则。
     *
     * @param agent Agent 主数据
     * @param workflowVersion 草稿版本
     * @return 校验结果
     */
    WorkflowValidationResult validate(AgentRecord agent, WorkflowVersionRecord workflowVersion);
}
