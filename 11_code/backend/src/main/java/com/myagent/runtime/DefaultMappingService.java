package com.myagent.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.myagent.common.error.BizException;
import com.myagent.common.error.ErrorCode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 默认映射服务。
 */
@Service
public class DefaultMappingService implements MappingService {

    /**
     * JSON 对象映射器。
     */
    private final ObjectMapper objectMapper;

    /**
     * 构造映射服务。
     *
     * @param objectMapper JSON 对象映射器
     */
    public DefaultMappingService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 从工作流上下文提取节点输入。
     *
     * @param workflowContext 工作流上下文根对象
     * @param inputMapping 输入映射定义
     * @return 节点输入
     */
    @Override
    public JsonNode extractInput(JsonNode workflowContext, JsonNode inputMapping) {
        if (inputMapping == null || inputMapping.isNull() || inputMapping.isMissingNode()) {
            return workflowContext.deepCopy();
        }
        if (inputMapping.isTextual()) {
            return readRequired(workflowContext, inputMapping.asText()).deepCopy();
        }
        if (!inputMapping.isObject()) {
            throw new BizException(ErrorCode.NODE_EXECUTION_FAILED, "输入映射必须是 JSON 对象或路径字符串。");
        }
        JsonNode pathNode = inputMapping.get("path");
        if (pathNode != null && pathNode.isTextual()) {
            return readRequired(workflowContext, pathNode.asText()).deepCopy();
        }
        ObjectNode input = objectMapper.createObjectNode();
        Iterator<Map.Entry<String, JsonNode>> fields = inputMapping.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            JsonNode mappingValue = field.getValue();
            String sourcePath = mappingValue.isTextual() ? mappingValue.asText() : mappingValue.path("path").asText(null);
            if (sourcePath == null || sourcePath.isBlank()) {
                throw new BizException(ErrorCode.NODE_EXECUTION_FAILED, "输入映射字段缺少来源路径。");
            }
            input.set(field.getKey(), readRequired(workflowContext, sourcePath).deepCopy());
        }
        return input;
    }

    /**
     * 将节点输出写回工作流上下文。
     *
     * @param workflowContext 工作流上下文根对象
     * @param outputMapping 输出映射定义
     * @param nodeOutput 节点输出
     * @return 写回后的上下文
     */
    @Override
    public JsonNode applyOutput(JsonNode workflowContext, JsonNode outputMapping, JsonNode nodeOutput) {
        ObjectNode mutableContext = requireObject(workflowContext, "工作流上下文必须是对象。").deepCopy();
        if (outputMapping == null || outputMapping.isNull() || outputMapping.isMissingNode()) {
            writeValue(mutableContext, "$.output", nodeOutput);
            return mutableContext;
        }
        if (outputMapping.isTextual()) {
            writeValue(mutableContext, outputMapping.asText(), nodeOutput);
            return mutableContext;
        }
        if (!outputMapping.isObject()) {
            throw new BizException(ErrorCode.NODE_EXECUTION_FAILED, "输出映射必须是 JSON 对象或路径字符串。");
        }
        Iterator<Map.Entry<String, JsonNode>> fields = outputMapping.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            JsonNode mappingValue = field.getValue();
            String targetPath = field.getKey();
            String sourcePath = mappingValue.isTextual() ? mappingValue.asText() : mappingValue.path("path").asText(null);
            if (sourcePath == null || sourcePath.isBlank()) {
                throw new BizException(ErrorCode.NODE_EXECUTION_FAILED, "输出映射字段缺少节点输出来源路径。");
            }
            JsonNode value = "$".equals(sourcePath) ? nodeOutput : readRequired(nodeOutput, sourcePath);
            writeValue(mutableContext, targetPath, value);
        }
        return mutableContext;
    }

    /**
     * 按受控 JSONPath 读取必填值。
     *
     * @param root 根节点
     * @param path 路径
     * @return 读取到的节点
     */
    private JsonNode readRequired(JsonNode root, String path) {
        JsonNode current = root;
        for (PathToken token : parsePath(path)) {
            if (token.fieldName() != null) {
                if (current == null || !current.isObject() || !current.has(token.fieldName())) {
                    throw new BizException(ErrorCode.NODE_EXECUTION_FAILED, "映射读取失败：" + path);
                }
                current = current.get(token.fieldName());
            } else {
                if (current == null || !current.isArray() || token.arrayIndex() >= current.size()) {
                    throw new BizException(ErrorCode.NODE_EXECUTION_FAILED, "映射数组下标越界：" + path);
                }
                current = current.get(token.arrayIndex());
            }
        }
        if (current == null || current instanceof MissingNode) {
            throw new BizException(ErrorCode.NODE_EXECUTION_FAILED, "映射读取失败：" + path);
        }
        return current;
    }

    /**
     * 写入受控 JSONPath。
     *
     * @param root 上下文根对象
     * @param path 目标路径
     * @param value 写入值
     */
    private void writeValue(ObjectNode root, String path, JsonNode value) {
        if ("$.input".equals(path) || path.startsWith("$.input.")) {
            throw new BizException(ErrorCode.NODE_EXECUTION_FAILED, "输出映射不允许覆盖 $.input。");
        }
        List<PathToken> tokens = parsePath(path);
        if (tokens.isEmpty()) {
            throw new BizException(ErrorCode.NODE_EXECUTION_FAILED, "输出映射不能覆盖上下文根对象。");
        }
        JsonNode current = root;
        for (int index = 0; index < tokens.size() - 1; index++) {
            PathToken token = tokens.get(index);
            PathToken nextToken = tokens.get(index + 1);
            if (token.fieldName() == null) {
                throw new BizException(ErrorCode.NODE_EXECUTION_FAILED, "输出映射不自动创建数组。");
            }
            ObjectNode objectNode = requireObject(current, "输出映射中间路径类型冲突。");
            JsonNode child = objectNode.get(token.fieldName());
            if (child == null || child.isNull() || child.isMissingNode()) {
                if (nextToken.fieldName() == null) {
                    throw new BizException(ErrorCode.NODE_EXECUTION_FAILED, "输出映射不自动创建数组。");
                }
                child = objectMapper.createObjectNode();
                objectNode.set(token.fieldName(), child);
            }
            if (!child.isObject()) {
                throw new BizException(ErrorCode.NODE_EXECUTION_FAILED, "输出映射中间路径类型冲突。");
            }
            current = child;
        }
        PathToken leaf = tokens.get(tokens.size() - 1);
        if (leaf.fieldName() == null) {
            ArrayNode arrayNode = requireArray(current, "输出映射目标数组不存在。");
            if (leaf.arrayIndex() >= arrayNode.size()) {
                throw new BizException(ErrorCode.NODE_EXECUTION_FAILED, "输出映射不自动创建数组。");
            }
            arrayNode.set(leaf.arrayIndex(), value == null ? objectMapper.nullNode() : value.deepCopy());
            return;
        }
        requireObject(current, "输出映射目标父路径必须是对象。")
                .set(leaf.fieldName(), value == null ? objectMapper.nullNode() : value.deepCopy());
    }

    /**
     * 解析受控 JSONPath。
     *
     * @param path JSONPath 字符串
     * @return 路径片段
     */
    private List<PathToken> parsePath(String path) {
        if (path == null || path.isBlank() || path.charAt(0) != '$') {
            throw new BizException(ErrorCode.NODE_EXECUTION_FAILED, "仅支持以 $ 开头的 JSONPath。");
        }
        List<PathToken> tokens = new ArrayList<>();
        int index = 1;
        while (index < path.length()) {
            char current = path.charAt(index);
            if (current == '.') {
                int start = ++index;
                while (index < path.length() && path.charAt(index) != '.' && path.charAt(index) != '[') {
                    index++;
                }
                if (start == index) {
                    throw new BizException(ErrorCode.NODE_EXECUTION_FAILED, "JSONPath 点路径不能为空。");
                }
                tokens.add(new PathToken(path.substring(start, index), null));
                continue;
            }
            if (current == '[') {
                int end = path.indexOf(']', index);
                if (end < 0) {
                    throw new BizException(ErrorCode.NODE_EXECUTION_FAILED, "JSONPath 数组下标格式不正确。");
                }
                String rawIndex = path.substring(index + 1, end);
                try {
                    int arrayIndex = Integer.parseInt(rawIndex);
                    if (arrayIndex < 0) {
                        throw new NumberFormatException("数组下标不能为负数。");
                    }
                    tokens.add(new PathToken(null, arrayIndex));
                } catch (NumberFormatException exception) {
                    throw new BizException(ErrorCode.NODE_EXECUTION_FAILED, "JSONPath 数组下标必须是非负整数。");
                }
                index = end + 1;
                continue;
            }
            throw new BizException(ErrorCode.NODE_EXECUTION_FAILED, "JSONPath 只支持点路径和数组下标。");
        }
        return tokens;
    }

    /**
     * 要求节点为对象。
     *
     * @param node JSON 节点
     * @param message 错误消息
     * @return 对象节点
     */
    private ObjectNode requireObject(JsonNode node, String message) {
        if (node == null || !node.isObject()) {
            throw new BizException(ErrorCode.NODE_EXECUTION_FAILED, message);
        }
        return (ObjectNode) node;
    }

    /**
     * 要求节点为数组。
     *
     * @param node JSON 节点
     * @param message 错误消息
     * @return 数组节点
     */
    private ArrayNode requireArray(JsonNode node, String message) {
        if (node == null || !node.isArray()) {
            throw new BizException(ErrorCode.NODE_EXECUTION_FAILED, message);
        }
        return (ArrayNode) node;
    }

    /**
     * JSONPath 路径片段。
     *
     * @param fieldName 字段名
     * @param arrayIndex 数组下标
     */
    private record PathToken(String fieldName, Integer arrayIndex) {
    }
}
