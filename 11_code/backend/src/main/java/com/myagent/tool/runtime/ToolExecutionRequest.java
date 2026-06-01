package com.myagent.tool.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.myagent.tool.repository.ToolRecord;

/**
 * 工具执行请求。
 *
 * @param tool 工具主数据
 * @param input 节点输入
 */
public record ToolExecutionRequest(
        ToolRecord tool,
        JsonNode input
) {
}
