package com.myagent.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * 工作流运行上下文。
 */
public class WorkflowContext {

    /**
     * JSON 对象映射器。
     */
    private final ObjectMapper objectMapper;

    /**
     * 上下文根对象。
     */
    private final ObjectNode root;

    /**
     * 构造工作流上下文。
     *
     * @param objectMapper JSON 对象映射器
     * @param input 运行输入
     */
    public WorkflowContext(ObjectMapper objectMapper, JsonNode input) {
        this.objectMapper = objectMapper;
        this.root = objectMapper.createObjectNode();
        this.root.set("input", input == null ? objectMapper.createObjectNode() : input.deepCopy());
        this.root.set("nodes", objectMapper.createObjectNode());
    }

    /**
     * 返回上下文根对象。
     *
     * @return 上下文根对象
     */
    public ObjectNode root() {
        return root;
    }

    /**
     * 用新的根对象替换当前上下文内容。
     *
     * @param newRoot 新根对象
     */
    public void replaceRoot(JsonNode newRoot) {
        root.removeAll();
        if (newRoot != null && newRoot.isObject()) {
            newRoot.fields().forEachRemaining(entry -> root.set(entry.getKey(), entry.getValue().deepCopy()));
        }
    }

    /**
     * 返回运行输入。
     *
     * @return 运行输入
     */
    public JsonNode input() {
        return root.get("input");
    }

    /**
     * 记录节点输出。
     *
     * @param nodeId 节点标识
     * @param output 节点输出
     */
    public void putNodeOutput(String nodeId, JsonNode output) {
        ObjectNode nodesNode = (ObjectNode) root.withObject("/nodes");
        nodesNode.set(nodeId, output == null ? objectMapper.nullNode() : output.deepCopy());
    }

    /**
     * 写入最终输出。
     *
     * @param output 最终输出
     */
    public void setOutput(JsonNode output) {
        root.set("output", output == null ? objectMapper.nullNode() : output.deepCopy());
    }

    /**
     * 返回最终输出。
     *
     * @return 最终输出
     */
    public JsonNode output() {
        return root.get("output");
    }
}
