package com.myagent.tool.runtime;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 工具执行器。
 */
public interface ToolExecutor {

    /**
     * 返回执行器类型。
     *
     * @return 执行器类型
     */
    String executorType();

    /**
     * 执行工具。
     *
     * @param request 工具执行请求
     * @return 工具输出
     */
    JsonNode execute(ToolExecutionRequest request);
}
