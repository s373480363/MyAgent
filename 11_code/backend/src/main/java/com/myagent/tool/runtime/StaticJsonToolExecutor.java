package com.myagent.tool.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/**
 * 静态 JSON 工具执行器。
 */
@Component
public class StaticJsonToolExecutor implements ToolExecutor {

    /**
     * JSON 对象映射器。
     */
    private final ObjectMapper objectMapper;

    /**
     * 构造静态 JSON 工具执行器。
     *
     * @param objectMapper JSON 对象映射器
     */
    public StaticJsonToolExecutor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 返回执行器类型。
     *
     * @return 执行器类型
     */
    @Override
    public String executorType() {
        return "STATIC_JSON";
    }

    /**
     * 执行静态 JSON 工具。
     *
     * @param request 工具执行请求
     * @return 静态 JSON 输出
     */
    @Override
    public JsonNode execute(ToolExecutionRequest request) {
        JsonNode config = request.tool().executorConfigJson();
        return config != null && config.has("output") ? config.get("output") : objectMapper.createObjectNode();
    }
}
