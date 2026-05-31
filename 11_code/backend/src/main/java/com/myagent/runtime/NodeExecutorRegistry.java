package com.myagent.runtime;

import com.myagent.workflow.domain.WorkflowNodeType;

/**
 * 节点执行器注册表。
 */
public interface NodeExecutorRegistry {

    /**
     * 查询节点执行器。
     *
     * @param nodeType 节点类型
     * @return 节点执行器
     */
    NodeExecutor getExecutor(WorkflowNodeType nodeType);

    /**
     * 判断是否支持节点类型。
     *
     * @param nodeType 节点类型
     * @return 支持时返回 true
     */
    boolean supports(WorkflowNodeType nodeType);
}
