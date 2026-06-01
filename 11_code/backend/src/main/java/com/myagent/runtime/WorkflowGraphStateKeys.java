package com.myagent.runtime;

/**
 * LangGraph4j 工作流图状态键。
 */
final class WorkflowGraphStateKeys {

    /**
     * 图状态中的工作流上下文键。
     */
    static final String WORKFLOW_CONTEXT = "workflowContext";

    /**
     * 图状态中的当前执行步数键。
     */
    static final String CURRENT_STEP = "currentStep";

    /**
     * 图状态中的下一节点标识键。
     */
    static final String NEXT_NODE_ID = "nextNodeId";

    /**
     * 图状态中的运行结果键。
     */
    static final String RUNTIME_RESULT = "runtimeResult";

    /**
     * 图状态中的运行期执行标识键。
     */
    static final String EXECUTION_CONTEXT_ID = "executionContextId";

    /**
     * LangGraph4j 结束节点。
     */
    static final String LANGGRAPH_END = "__END__";

    /**
     * 禁止实例化。
     */
    private WorkflowGraphStateKeys() {
    }
}
