package com.myagent.schema.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.myagent.common.error.BizException;
import com.myagent.common.error.ErrorCode;
import com.myagent.schema.repository.SchemaRecord;
import com.myagent.schema.repository.SchemaRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 默认 Schema 校验服务。
 */
@Service
public class DefaultSchemaValidationService implements SchemaValidationService {

    /**
     * Schema 仓储。
     */
    private final SchemaRepository schemaRepository;

    /**
     * 构造 Schema 校验服务。
     *
     * @param schemaRepository Schema 仓储
     */
    public DefaultSchemaValidationService(SchemaRepository schemaRepository) {
        this.schemaRepository = schemaRepository;
    }

    /**
     * 按 Schema 引用校验业务载荷。
     *
     * @param payload 待校验业务载荷
     * @param schemaRef Schema 引用
     * @param stage 校验阶段
     * @return 校验结果
     */
    @Override
    public SchemaValidationResult validate(JsonNode payload, SchemaReference schemaRef, ValidationStage stage) {
        SchemaRecord schema = loadSchema(schemaRef);
        List<SchemaValidationError> errors = new ArrayList<>();
        validateNode(schema.getJsonSchema(), payload, "$", errors);
        if (errors.isEmpty()) {
            return SchemaValidationResult.valid(schema.getSchemaKey(), schema.getVersion());
        }
        return SchemaValidationResult.invalid(schema.getSchemaKey(), schema.getVersion(), errors);
    }

    /**
     * 加载 Schema 记录。
     *
     * @param schemaRef Schema 引用
     * @return Schema 记录
     */
    private SchemaRecord loadSchema(SchemaReference schemaRef) {
        if (schemaRef == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Schema 引用不能为空。");
        }
        if (schemaRef.getSchemaId() != null) {
            return schemaRepository.findById(schemaRef.getSchemaId())
                    .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND, "指定 Schema 不存在。"));
        }
        if (schemaRef.getSchemaKey() != null && schemaRef.getVersion() != null) {
            return schemaRepository.findByKeyAndVersion(schemaRef.getSchemaKey(), schemaRef.getVersion())
                    .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND, "指定 Schema 不存在。"));
        }
        throw new BizException(ErrorCode.INVALID_ARGUMENT, "Schema 引用必须包含 schemaId 或 schemaKey + version。");
    }

    /**
     * 递归校验节点。
     *
     * @param schemaNode Schema 节点
     * @param payloadNode 数据节点
     * @param path 当前路径
     * @param errors 错误列表
     */
    private void validateNode(
            JsonNode schemaNode,
            JsonNode payloadNode,
            String path,
            List<SchemaValidationError> errors
    ) {
        if (schemaNode == null || !schemaNode.isObject()) {
            return;
        }
        validateType(schemaNode, payloadNode, path, errors);
        validateEnum(schemaNode, payloadNode, path, errors);
        validateString(schemaNode, payloadNode, path, errors);
        validateNumber(schemaNode, payloadNode, path, errors);
        validateObject(schemaNode, payloadNode, path, errors);
        validateArray(schemaNode, payloadNode, path, errors);
    }

    /**
     * 校验类型。
     *
     * @param schemaNode Schema 节点
     * @param payloadNode 数据节点
     * @param path 当前路径
     * @param errors 错误列表
     */
    private void validateType(JsonNode schemaNode, JsonNode payloadNode, String path, List<SchemaValidationError> errors) {
        JsonNode typeNode = schemaNode.get("type");
        if (typeNode == null) {
            return;
        }
        if (matchesAnyType(typeNode, payloadNode)) {
            return;
        }
        errors.add(new SchemaValidationError(path, "type", "字段类型不符合 Schema 要求。"));
    }

    /**
     * 判断数据节点是否匹配任一类型。
     *
     * @param typeNode type 节点
     * @param payloadNode 数据节点
     * @return 匹配时返回 true
     */
    private boolean matchesAnyType(JsonNode typeNode, JsonNode payloadNode) {
        if (typeNode.isTextual()) {
            return matchesType(typeNode.asText(), payloadNode);
        }
        if (!typeNode.isArray()) {
            return true;
        }
        for (JsonNode item : typeNode) {
            if (item.isTextual() && matchesType(item.asText(), payloadNode)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断数据节点是否匹配指定类型。
     *
     * @param type 类型名
     * @param payloadNode 数据节点
     * @return 匹配时返回 true
     */
    private boolean matchesType(String type, JsonNode payloadNode) {
        if (payloadNode == null || payloadNode.isMissingNode()) {
            return false;
        }
        return switch (type) {
            case "object" -> payloadNode.isObject();
            case "string" -> payloadNode.isTextual();
            case "number" -> payloadNode.isNumber();
            case "integer" -> payloadNode.isIntegralNumber();
            case "boolean" -> payloadNode.isBoolean();
            case "array" -> payloadNode.isArray();
            case "null" -> payloadNode.isNull();
            default -> true;
        };
    }

    /**
     * 校验枚举。
     *
     * @param schemaNode Schema 节点
     * @param payloadNode 数据节点
     * @param path 当前路径
     * @param errors 错误列表
     */
    private void validateEnum(JsonNode schemaNode, JsonNode payloadNode, String path, List<SchemaValidationError> errors) {
        JsonNode enumNode = schemaNode.get("enum");
        if (enumNode == null || !enumNode.isArray()) {
            return;
        }
        for (JsonNode allowedValue : enumNode) {
            if (allowedValue.equals(payloadNode)) {
                return;
            }
        }
        errors.add(new SchemaValidationError(path, "enum", "字段值不在允许范围内。"));
    }

    /**
     * 校验字符串约束。
     *
     * @param schemaNode Schema 节点
     * @param payloadNode 数据节点
     * @param path 当前路径
     * @param errors 错误列表
     */
    private void validateString(JsonNode schemaNode, JsonNode payloadNode, String path, List<SchemaValidationError> errors) {
        if (payloadNode == null || !payloadNode.isTextual()) {
            return;
        }
        int length = payloadNode.asText().length();
        JsonNode minLength = schemaNode.get("minLength");
        if (minLength != null && minLength.isInt() && length < minLength.asInt()) {
            errors.add(new SchemaValidationError(path, "minLength", "字段长度不能小于 " + minLength.asInt() + "。"));
        }
        JsonNode maxLength = schemaNode.get("maxLength");
        if (maxLength != null && maxLength.isInt() && length > maxLength.asInt()) {
            errors.add(new SchemaValidationError(path, "maxLength", "字段长度不能大于 " + maxLength.asInt() + "。"));
        }
        JsonNode pattern = schemaNode.get("pattern");
        if (pattern != null && pattern.isTextual() && !payloadNode.asText().matches(pattern.asText())) {
            errors.add(new SchemaValidationError(path, "pattern", "字段格式不符合正则约束。"));
        }
    }

    /**
     * 校验数值约束。
     *
     * @param schemaNode Schema 节点
     * @param payloadNode 数据节点
     * @param path 当前路径
     * @param errors 错误列表
     */
    private void validateNumber(JsonNode schemaNode, JsonNode payloadNode, String path, List<SchemaValidationError> errors) {
        if (payloadNode == null || !payloadNode.isNumber()) {
            return;
        }
        BigDecimal value = payloadNode.decimalValue();
        JsonNode minimum = schemaNode.get("minimum");
        if (minimum != null && minimum.isNumber() && value.compareTo(minimum.decimalValue()) < 0) {
            errors.add(new SchemaValidationError(path, "minimum", "字段值不能小于 " + minimum.asText() + "。"));
        }
        JsonNode maximum = schemaNode.get("maximum");
        if (maximum != null && maximum.isNumber() && value.compareTo(maximum.decimalValue()) > 0) {
            errors.add(new SchemaValidationError(path, "maximum", "字段值不能大于 " + maximum.asText() + "。"));
        }
    }

    /**
     * 校验对象约束。
     *
     * @param schemaNode Schema 节点
     * @param payloadNode 数据节点
     * @param path 当前路径
     * @param errors 错误列表
     */
    private void validateObject(JsonNode schemaNode, JsonNode payloadNode, String path, List<SchemaValidationError> errors) {
        if (payloadNode == null || !payloadNode.isObject()) {
            return;
        }
        JsonNode requiredNode = schemaNode.get("required");
        if (requiredNode != null && requiredNode.isArray()) {
            for (JsonNode requiredField : requiredNode) {
                if (requiredField.isTextual() && !payloadNode.has(requiredField.asText())) {
                    errors.add(new SchemaValidationError(joinPath(path, requiredField.asText()), "required", "请填写必填字段。"));
                }
            }
        }
        JsonNode propertiesNode = schemaNode.get("properties");
        if (propertiesNode != null && propertiesNode.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = propertiesNode.properties().iterator();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                if (payloadNode.has(entry.getKey())) {
                    validateNode(entry.getValue(), payloadNode.get(entry.getKey()), joinPath(path, entry.getKey()), errors);
                }
            }
        }
        JsonNode additionalProperties = schemaNode.get("additionalProperties");
        if (additionalProperties != null && additionalProperties.isBoolean() && !additionalProperties.asBoolean()) {
            validateAdditionalProperties(propertiesNode, payloadNode, path, errors);
        }
    }

    /**
     * 校验额外字段。
     *
     * @param propertiesNode 属性定义
     * @param payloadNode 数据节点
     * @param path 当前路径
     * @param errors 错误列表
     */
    private void validateAdditionalProperties(
            JsonNode propertiesNode,
            JsonNode payloadNode,
            String path,
            List<SchemaValidationError> errors
    ) {
        Iterator<String> fieldNames = payloadNode.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            if (propertiesNode == null || !propertiesNode.has(fieldName)) {
                errors.add(new SchemaValidationError(joinPath(path, fieldName), "additionalProperties", "不允许提交未定义字段。"));
            }
        }
    }

    /**
     * 校验数组约束。
     *
     * @param schemaNode Schema 节点
     * @param payloadNode 数据节点
     * @param path 当前路径
     * @param errors 错误列表
     */
    private void validateArray(JsonNode schemaNode, JsonNode payloadNode, String path, List<SchemaValidationError> errors) {
        if (payloadNode == null || !payloadNode.isArray()) {
            return;
        }
        JsonNode itemsNode = schemaNode.get("items");
        if (itemsNode == null || !itemsNode.isObject()) {
            return;
        }
        for (int index = 0; index < payloadNode.size(); index++) {
            validateNode(itemsNode, payloadNode.get(index), path + "[" + index + "]", errors);
        }
    }

    /**
     * 拼接 JSON 路径。
     *
     * @param parent 父路径
     * @param child 子字段
     * @return 完整路径
     */
    private String joinPath(String parent, String child) {
        if ("$".equals(parent)) {
            return "$." + child;
        }
        return parent + "." + child;
    }
}
