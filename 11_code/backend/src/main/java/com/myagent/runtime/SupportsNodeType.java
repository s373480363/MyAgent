package com.myagent.runtime;

import com.myagent.workflow.domain.WorkflowNodeType;

/**
 * 节点执行器支持类型声明。
 */
public interface SupportsNodeType {

    /**
     * 返回支持的节点类型。
     *
     * @return 节点类型
     */
    WorkflowNodeType supportedNodeType();
}
