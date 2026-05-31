package com.myagent.workflow.domain;

/**
 * 工作流边类型。
 */
public enum WorkflowEdgeType {

    /**
     * 普通边。
     */
    NORMAL,

    /**
     * 条件边。
     */
    CONDITION,

    /**
     * 默认边。
     */
    DEFAULT,

    /**
     * 结束边。
     */
    END
}
