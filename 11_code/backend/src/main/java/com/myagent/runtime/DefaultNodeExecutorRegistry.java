package com.myagent.runtime;

import com.myagent.common.error.BizException;
import com.myagent.common.error.ErrorCode;
import com.myagent.workflow.domain.WorkflowNodeType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 默认节点执行器注册表。
 */
@Component
public class DefaultNodeExecutorRegistry implements NodeExecutorRegistry {

    /**
     * 节点执行器索引。
     */
    private final Map<WorkflowNodeType, NodeExecutor> executors;

    /**
     * 构造节点执行器注册表。
     *
     * @param candidates 候选节点执行器
     */
    public DefaultNodeExecutorRegistry(List<NodeExecutor> candidates) {
        Map<WorkflowNodeType, NodeExecutor> executorMap = new EnumMap<>(WorkflowNodeType.class);
        for (NodeExecutor candidate : candidates) {
            if (candidate instanceof SupportsNodeType supportsNodeType) {
                WorkflowNodeType nodeType = supportsNodeType.supportedNodeType();
                if (executorMap.put(nodeType, candidate) != null) {
                    throw new BizException(ErrorCode.INTERNAL_ERROR, "节点执行器注册重复：" + nodeType);
                }
            }
        }
        this.executors = Map.copyOf(executorMap);
    }

    /**
     * 查询节点执行器。
     *
     * @param nodeType 节点类型
     * @return 节点执行器
     */
    @Override
    public NodeExecutor getExecutor(WorkflowNodeType nodeType) {
        NodeExecutor executor = executors.get(nodeType);
        if (executor == null) {
            throw new BizException(ErrorCode.NODE_EXECUTION_FAILED, "暂不支持节点类型：" + nodeType);
        }
        return executor;
    }

    /**
     * 判断是否支持节点类型。
     *
     * @param nodeType 节点类型
     * @return 支持时返回 true
     */
    @Override
    public boolean supports(WorkflowNodeType nodeType) {
        return executors.containsKey(nodeType);
    }
}
