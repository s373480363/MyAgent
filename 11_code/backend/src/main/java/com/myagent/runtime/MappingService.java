package com.myagent.runtime;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 节点输入输出映射服务。
 */
public interface MappingService {

    /**
     * 从工作流上下文提取节点输入。
     *
     * @param workflowContext 工作流上下文根对象
     * @param inputMapping 输入映射定义
     * @return 节点输入
     */
    JsonNode extractInput(JsonNode workflowContext, JsonNode inputMapping);

    /**
     * 将节点输出写回工作流上下文。
     *
     * @param workflowContext 工作流上下文根对象
     * @param outputMapping 输出映射定义
     * @param nodeOutput 节点输出
     * @return 写回后的上下文
     */
    JsonNode applyOutput(JsonNode workflowContext, JsonNode outputMapping, JsonNode nodeOutput);
}
