package com.myagent.runtime;

import com.myagent.workflow.application.result.WorkflowValidationResult;

/**
 * 运行时工作流校验器。
 */
public interface WorkflowValidator {

    /**
     * 校验草稿版本。
     *
     * @param snapshot 工作流版本快照
     * @return 校验结果
     */
    WorkflowValidationResult validateDraft(WorkflowVersionSnapshot snapshot);

    /**
     * 校验发布版本。
     *
     * @param snapshot 工作流版本快照
     * @return 校验结果
     */
    WorkflowValidationResult validateForPublish(WorkflowVersionSnapshot snapshot);
}
