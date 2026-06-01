package com.myagent.runtime;

import com.myagent.common.error.BizException;
import com.myagent.common.error.ErrorCode;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * LangGraph4j 运行期执行回调登记表。
 */
final class WorkflowGraphExecutionRegistry {

    /**
     * 执行上下文索引。
     */
    private static final ConcurrentMap<String, WorkflowGraphExecutionContext> CONTEXTS = new ConcurrentHashMap<>();

    /**
     * 登记执行上下文。
     *
     * @param executionId 执行标识
     * @param context 执行上下文
     */
    static void register(String executionId, WorkflowGraphExecutionContext context) {
        CONTEXTS.put(executionId, context);
    }

    /**
     * 查询执行上下文。
     *
     * @param executionId 执行标识
     * @return 执行上下文
     */
    static WorkflowGraphExecutionContext required(String executionId) {
        WorkflowGraphExecutionContext context = CONTEXTS.get(executionId);
        if (context == null) {
            throw new BizException(ErrorCode.NODE_EXECUTION_FAILED, "工作流图运行期执行上下文不存在。");
        }
        return context;
    }

    /**
     * 注销执行上下文。
     *
     * @param executionId 执行标识
     */
    static void unregister(String executionId) {
        CONTEXTS.remove(executionId);
    }

    /**
     * 禁止实例化。
     */
    private WorkflowGraphExecutionRegistry() {
    }
}
