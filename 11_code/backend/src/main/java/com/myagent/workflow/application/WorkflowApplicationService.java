package com.myagent.workflow.application;

import com.myagent.common.page.PageResult;
import com.myagent.workflow.application.command.CopyWorkflowDraftFromVersionCommand;
import com.myagent.workflow.application.command.PublishWorkflowDraftCommand;
import com.myagent.workflow.application.command.SaveWorkflowDraftCommand;
import com.myagent.workflow.application.command.ValidateWorkflowDraftCommand;
import com.myagent.workflow.application.query.GetWorkflowDraftQuery;
import com.myagent.workflow.application.query.GetWorkflowVersionQuery;
import com.myagent.workflow.application.query.ListWorkflowVersionsQuery;
import com.myagent.workflow.application.result.WorkflowDraftResult;
import com.myagent.workflow.application.result.WorkflowPublishResult;
import com.myagent.workflow.application.result.WorkflowValidationResult;
import com.myagent.workflow.application.result.WorkflowVersionListItemResult;
import com.myagent.workflow.application.result.WorkflowVersionResult;

/**
 * 工作流应用服务接口。
 */
public interface WorkflowApplicationService {

    /**
     * 获取当前草稿。
     *
     * @param query 查询条件
     * @return 当前草稿
     */
    WorkflowDraftResult getWorkflowDraft(GetWorkflowDraftQuery query);

    /**
     * 保存当前草稿。
     *
     * @param command 保存命令
     * @return 新草稿
     */
    WorkflowDraftResult saveWorkflowDraft(SaveWorkflowDraftCommand command);

    /**
     * 从已有版本复制生成新草稿。
     *
     * @param command 复制命令
     * @return 新草稿
     */
    WorkflowDraftResult copyWorkflowDraftFromVersion(CopyWorkflowDraftFromVersionCommand command);

    /**
     * 校验当前草稿。
     *
     * @param command 校验命令
     * @return 校验结果
     */
    WorkflowValidationResult validateWorkflowDraft(ValidateWorkflowDraftCommand command);

    /**
     * 发布当前草稿。
     *
     * @param command 发布命令
     * @return 发布结果
     */
    WorkflowPublishResult publishWorkflowDraft(PublishWorkflowDraftCommand command);

    /**
     * 分页查询工作流版本。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    PageResult<WorkflowVersionListItemResult> listWorkflowVersions(ListWorkflowVersionsQuery query);

    /**
     * 查询工作流版本详情。
     *
     * @param query 查询条件
     * @return 版本详情
     */
    WorkflowVersionResult getWorkflowVersion(GetWorkflowVersionQuery query);
}
