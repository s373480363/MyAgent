package com.myagent.runtime;

/**
 * 工作流编译器。
 */
public interface WorkflowCompiler {

    /**
     * 编译工作流版本快照。
     *
     * @param snapshot 工作流版本快照
     * @return 编译后的工作流
     */
    CompiledWorkflow compile(WorkflowVersionSnapshot snapshot);
}
