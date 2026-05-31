package com.myagent.workflow.domain;

/**
 * 工作流版本状态。
 */
public enum WorkflowVersionStatus {

    /**
     * 当前草稿版本。
     */
    DRAFT,

    /**
     * 当前发布版本。
     */
    PUBLISHED,

    /**
     * 历史只读版本。
     */
    HISTORY
}
