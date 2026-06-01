package com.myagent.runtime;

import com.myagent.workflow.domain.WorkflowNodeDefinition;
import org.bsc.langgraph4j.state.AgentState;

import java.util.Map;

/**
 * 已编译工作流图的运行期节点执行回调。
 */
public interface WorkflowGraphExecutionContext {

    /**
     * 执行图中的单个节点。
     *
     * @param state 图状态
     * @param node 当前节点
     * @return 状态更新
     */
    Map<String, Object> executeGraphNode(AgentState state, WorkflowNodeDefinition node);
}
