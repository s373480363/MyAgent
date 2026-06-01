package com.myagent.tool.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

/**
 * 回显工具执行器。
 */
@Component
public class EchoToolExecutor implements ToolExecutor {

    /**
     * 返回执行器类型。
     *
     * @return 执行器类型
     */
    @Override
    public String executorType() {
        return "ECHO";
    }

    /**
     * 执行回显工具。
     *
     * @param request 工具执行请求
     * @return 原样返回节点输入
     */
    @Override
    public JsonNode execute(ToolExecutionRequest request) {
        return request.input();
    }
}
