package com.myagent.runtime;

/**
 * 节点执行器。
 */
public interface NodeExecutor {

    /**
     * 执行节点。
     *
     * @param context 节点执行上下文
     * @return 节点执行结果
     */
    NodeExecutionResult execute(NodeExecutionContext context);
}
