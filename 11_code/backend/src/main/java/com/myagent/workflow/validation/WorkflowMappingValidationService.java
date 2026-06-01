package com.myagent.workflow.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.myagent.common.api.ApiError;
import com.myagent.common.error.ErrorCode;
import com.myagent.schema.repository.SchemaRecord;
import com.myagent.schema.repository.SchemaRepository;
import com.myagent.workflow.application.result.WorkflowValidationIssueResult;
import com.myagent.workflow.domain.WorkflowNodeDefinition;
import com.myagent.workflow.domain.WorkflowSchemaRef;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 工作流映射与 Schema 字段关系校验服务。
 */
@Service
public class WorkflowMappingValidationService {

    /**
     * Schema 仓储。
     */
    private final SchemaRepository schemaRepository;

    /**
     * 构造映射校验服务。
     *
     * @param schemaRepository Schema 仓储
     */
    public WorkflowMappingValidationService(SchemaRepository schemaRepository) {
        this.schemaRepository = schemaRepository;
    }

    /**
     * 校验节点输入输出映射。
     *
     * @param node 节点定义
     * @return 校验问题列表
     */
    public List<WorkflowValidationIssueResult> validateMappings(WorkflowNodeDefinition node) {
        List<WorkflowValidationIssueResult> issues = new ArrayList<>();
        validateInputMapping(node, issues);
        validateOutputMapping(node, issues);
        return issues;
    }

    /**
     * 校验输入映射。
     *
     * @param node 节点定义
     * @param issues 校验问题列表
     */
    private void validateInputMapping(WorkflowNodeDefinition node, List<WorkflowValidationIssueResult> issues) {
        JsonNode inputMapping = node.getInputMapping();
        if (inputMapping == null || inputMapping.isNull() || inputMapping.isMissingNode()) {
            return;
        }
        Optional<SchemaRecord> schemaRecord = findSchema(node.getInputSchemaRef());
        if (inputMapping.isTextual()) {
            isPathSyntaxValid(inputMapping.asText(), "$.nodes[*].inputMapping", "inputMapping 路径语法不正确。", issues);
            return;
        }
        if (!inputMapping.isObject()) {
            issues.add(issue("$.nodes[*].inputMapping", "inputMapping 必须是对象或 JSONPath 字符串。", node.getNodeId()));
            return;
        }
        JsonNode pathNode = inputMapping.get("path");
        if (pathNode != null) {
            if (!pathNode.isTextual()) {
                issues.add(issue("$.nodes[*].inputMapping.path", "inputMapping.path 必须是 JSONPath 字符串。", node.getNodeId()));
                return;
            }
            isPathSyntaxValid(pathNode.asText(), "$.nodes[*].inputMapping.path", "inputMapping.path 语法不正确。", issues);
            return;
        }
        Set<String> mappedFields = new HashSet<>();
        Iterator<Map.Entry<String, JsonNode>> fields = inputMapping.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            mappedFields.add(field.getKey());
            if (schemaRecord.isPresent() && !hasRootProperty(schemaRecord.get().getJsonSchema(), field.getKey())) {
                issues.add(issue(
                        "$.nodes[*].inputMapping." + field.getKey(),
                        "inputMapping 目标字段不存在于 inputSchema.properties。",
                        node.getNodeId()
                ));
            }
            String sourcePath = mappingPath(field.getValue(), "path");
            if (sourcePath == null) {
                issues.add(issue("$.nodes[*].inputMapping." + field.getKey(), "inputMapping 字段缺少来源 JSONPath。", node.getNodeId()));
            } else {
                isPathSyntaxValid(sourcePath, "$.nodes[*].inputMapping." + field.getKey(), "inputMapping 来源路径语法不正确。", issues);
            }
        }
        if (schemaRecord.isPresent()) {
            for (String requiredField : requiredFields(schemaRecord.get().getJsonSchema())) {
                if (!mappedFields.contains(requiredField)) {
                    issues.add(issue(
                            "$.nodes[*].inputMapping",
                            "inputMapping 缺少 inputSchema 必填字段：" + requiredField,
                            node.getNodeId()
                    ));
                }
            }
        }
    }

    /**
     * 校验输出映射。
     *
     * @param node 节点定义
     * @param issues 校验问题列表
     */
    private void validateOutputMapping(WorkflowNodeDefinition node, List<WorkflowValidationIssueResult> issues) {
        JsonNode outputMapping = node.getOutputMapping();
        if (outputMapping == null || outputMapping.isNull() || outputMapping.isMissingNode()) {
            return;
        }
        Optional<SchemaRecord> schemaRecord = findSchema(node.getOutputSchemaRef());
        if (outputMapping.isTextual()) {
            validateOutputTargetPath(outputMapping.asText(), "$.nodes[*].outputMapping", issues);
            return;
        }
        if (!outputMapping.isObject()) {
            issues.add(issue("$.nodes[*].outputMapping", "outputMapping 必须是对象或 JSONPath 字符串。", node.getNodeId()));
            return;
        }
        Iterator<Map.Entry<String, JsonNode>> fields = outputMapping.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String targetPath = field.getKey();
            validateOutputTargetPath(targetPath, "$.nodes[*].outputMapping." + targetPath, issues);
            String sourcePath = mappingPath(field.getValue(), "path");
            if (sourcePath == null) {
                issues.add(issue("$.nodes[*].outputMapping." + targetPath, "outputMapping 字段缺少节点输出来源 JSONPath。", node.getNodeId()));
                continue;
            }
            boolean sourcePathValid = isPathSyntaxValid(
                    sourcePath,
                    "$.nodes[*].outputMapping." + targetPath,
                    "outputMapping 来源路径语法不正确。",
                    issues
            );
            if (sourcePathValid && schemaRecord.isPresent() && !"$".equals(sourcePath) && !schemaContainsPath(schemaRecord.get().getJsonSchema(), sourcePath)) {
                issues.add(issue(
                        "$.nodes[*].outputMapping." + targetPath,
                        "outputMapping 来源字段不存在于 outputSchema：" + sourcePath,
                        node.getNodeId()
                ));
            }
        }
    }

    /**
     * 查询 Schema 记录。
     *
     * @param schemaRef Schema 引用
     * @return Schema 记录
     */
    private Optional<SchemaRecord> findSchema(WorkflowSchemaRef schemaRef) {
        if (schemaRef == null || schemaRef.getSchemaKey() == null || schemaRef.getVersion() == null) {
            return Optional.empty();
        }
        return schemaRepository.findByKeyAndVersion(schemaRef.getSchemaKey(), schemaRef.getVersion());
    }

    /**
     * 读取映射路径。
     *
     * @param mappingValue 映射值
     * @param objectPathField 对象形态中的路径字段名
     * @return JSONPath 字符串
     */
    private String mappingPath(JsonNode mappingValue, String objectPathField) {
        if (mappingValue == null || mappingValue.isNull()) {
            return null;
        }
        if (mappingValue.isTextual()) {
            String value = mappingValue.asText();
            return value == null || value.isBlank() ? null : value.trim();
        }
        if (mappingValue.isObject() && mappingValue.path(objectPathField).isTextual()) {
            String value = mappingValue.path(objectPathField).asText();
            return value == null || value.isBlank() ? null : value.trim();
        }
        return null;
    }

    /**
     * 校验输出目标路径。
     *
     * @param targetPath 输出目标路径
     * @param field 字段路径
     * @param issues 校验问题列表
     */
    private void validateOutputTargetPath(String targetPath, String field, List<WorkflowValidationIssueResult> issues) {
        isPathSyntaxValid(targetPath, field, "outputMapping 目标路径语法不正确。", issues);
        if ("$.input".equals(targetPath) || targetPath.startsWith("$.input.")) {
            issues.add(issue(field, "outputMapping 不允许写入 $.input。", targetPath));
        }
    }

    /**
     * 校验 JSONPath 语法。
     *
     * @param path JSONPath
     * @param field 字段路径
     * @param message 错误消息
     * @param issues 校验问题列表
     */
    private boolean isPathSyntaxValid(String path, String field, String message, List<WorkflowValidationIssueResult> issues) {
        try {
            parsePath(path);
            return true;
        } catch (IllegalArgumentException exception) {
            issues.add(issue(field, message + exception.getMessage(), path));
            return false;
        }
    }

    /**
     * 判断根对象是否声明字段。
     *
     * @param schemaJson JSON Schema
     * @param fieldName 字段名
     * @return 已声明时返回 true
     */
    private boolean hasRootProperty(JsonNode schemaJson, String fieldName) {
        JsonNode properties = schemaJson == null ? null : schemaJson.path("properties");
        return properties != null && properties.isObject() && properties.has(fieldName);
    }

    /**
     * 读取 required 字段集合。
     *
     * @param schemaJson JSON Schema
     * @return required 字段集合
     */
    private Set<String> requiredFields(JsonNode schemaJson) {
        Set<String> required = new HashSet<>();
        JsonNode requiredNode = schemaJson == null ? null : schemaJson.path("required");
        if (requiredNode != null && requiredNode.isArray()) {
            for (JsonNode field : requiredNode) {
                if (field.isTextual()) {
                    required.add(field.asText());
                }
            }
        }
        return required;
    }

    /**
     * 判断 JSON Schema 是否包含指定输出路径。
     *
     * @param schemaJson JSON Schema
     * @param path 输出路径
     * @return 包含时返回 true
     */
    private boolean schemaContainsPath(JsonNode schemaJson, String path) {
        JsonNode currentSchema = schemaJson;
        for (PathToken token : parsePath(path)) {
            if (token.fieldName() != null) {
                JsonNode properties = currentSchema == null ? null : currentSchema.path("properties");
                if (properties == null || !properties.isObject() || !properties.has(token.fieldName())) {
                    return false;
                }
                currentSchema = properties.get(token.fieldName());
                continue;
            }
            JsonNode items = currentSchema == null ? null : currentSchema.path("items");
            if (items == null || items.isMissingNode() || items.isNull()) {
                return false;
            }
            currentSchema = items;
        }
        return true;
    }

    /**
     * 解析受控 JSONPath。
     *
     * @param path JSONPath 字符串
     * @return 路径片段
     */
    private List<PathToken> parsePath(String path) {
        if (path == null || path.isBlank() || path.charAt(0) != '$') {
            throw new IllegalArgumentException("仅支持以 $ 开头的 JSONPath。");
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
                    throw new IllegalArgumentException("JSONPath 点路径不能为空。");
                }
                tokens.add(new PathToken(path.substring(start, index), null));
                continue;
            }
            if (current == '[') {
                int end = path.indexOf(']', index);
                if (end < 0) {
                    throw new IllegalArgumentException("JSONPath 数组下标格式不正确。");
                }
                String rawIndex = path.substring(index + 1, end);
                try {
                    int arrayIndex = Integer.parseInt(rawIndex);
                    if (arrayIndex < 0) {
                        throw new NumberFormatException("数组下标不能为负数。");
                    }
                    tokens.add(new PathToken(null, arrayIndex));
                } catch (NumberFormatException exception) {
                    throw new IllegalArgumentException("JSONPath 数组下标必须是非负整数。");
                }
                index = end + 1;
                continue;
            }
            throw new IllegalArgumentException("JSONPath 只支持点路径和数组下标。");
        }
        return tokens;
    }

    /**
     * 构造校验问题。
     *
     * @param field 字段路径
     * @param message 中文消息
     * @param actual 实际值
     * @return 校验问题
     */
    private WorkflowValidationIssueResult issue(String field, String message, String actual) {
        return new WorkflowValidationIssueResult(
                ErrorCode.WORKFLOW_VALIDATION_FAILED.getCode(),
                message,
                List.of(new ApiError.Detail(field, "invalid", message, null, actual, null))
        );
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
