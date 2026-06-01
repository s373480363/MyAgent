package com.myagent.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 工作流运行上下文。
 */
public class WorkflowContext implements Serializable {

    /**
     * 序列化版本。
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * JSON 对象映射器。
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
        List<String> fieldNames = new ArrayList<>();
        root.fieldNames().forEachRemaining(fieldNames::add);
        for (String fieldName : fieldNames) {
            root.remove(fieldName);
        }
        if (newRoot != null && newRoot.isObject()) {
            newRoot.fieldNames().forEachRemaining(fieldName -> root.set(fieldName, newRoot.get(fieldName).deepCopy()));
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
        JsonNode currentNodes = root.get("nodes");
        ObjectNode nodesNode;
        if (currentNodes instanceof ObjectNode objectNode) {
            nodesNode = objectNode;
        } else {
            nodesNode = OBJECT_MAPPER.createObjectNode();
            root.set("nodes", nodesNode);
        }
        nodesNode.set(nodeId, output == null ? OBJECT_MAPPER.nullNode() : output.deepCopy());
    }

    /**
     * 写入最终输出。
     *
     * @param output 最终输出
     */
    public void setOutput(JsonNode output) {
        root.set("output", output == null ? OBJECT_MAPPER.nullNode() : output.deepCopy());
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
