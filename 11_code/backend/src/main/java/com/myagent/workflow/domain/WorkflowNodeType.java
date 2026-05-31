package com.myagent.workflow.domain;

/**
 * 工作流节点类型。
 */
public enum WorkflowNodeType {

    /**
     * 开始节点。
     */
    START,

    /**
     * 大模型节点。
     */
    LLM,

    /**
     * 条件节点。
     */
    CONDITION,

    /**
     * Java 方法节点。
     */
    JAVA_METHOD,

    /**
     * 子 Agent 调用节点。
     */
    AGENT_CALL,

    /**
     * 外部 Agent 节点。
     */
    EXTERNAL_AGENT,

    /**
     * 工具节点。
     */
    TOOL,

    /**
     * 审核节点。
     */
    REVIEW,

    /**
     * 总结节点。
     */
    SUMMARY,

    /**
     * 结束节点。
     */
    END
}
