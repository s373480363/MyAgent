package com.myagent.schema.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.myagent.common.api.ApiError;
import com.myagent.common.error.BizException;
import com.myagent.common.error.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Schema 定义保存前校验器。
 */
@Component
public class SchemaDefinitionValidator {

    /**
     * V1 支持的根类型集合。
     */
    private static final Set<String> SUPPORTED_TYPES = Set.of(
            "object",
            "string",
            "number",
            "integer",
            "boolean",
            "array",
            "null"
    );

    /**
     * 校验 JSON Schema 定义是否满足 V1 受控子集的最小保存条件。
     *
     * @param jsonSchema JSON Schema 定义
     */
    public void validateDefinition(JsonNode jsonSchema) {
        List<ApiError.Detail> details = new ArrayList<>();
        if (jsonSchema == null || jsonSchema.isNull()) {
            details.add(ApiError.Detail.of("$.jsonSchema", "required", "JSON Schema 不能为空。"));
            throw invalid(details);
        }
        if (!jsonSchema.isObject()) {
            details.add(ApiError.Detail.of("$.jsonSchema", "type", "JSON Schema 根节点必须是对象。"));
            throw invalid(details);
        }
        validateSchemaNode(jsonSchema, "$.jsonSchema", details);
        if (!details.isEmpty()) {
            throw invalid(details);
        }
    }

    /**
     * 递归校验 Schema 节点结构。
     *
     * @param schemaNode Schema 节点
     * @param path 当前路径
     * @param details 错误明细
     */
    private void validateSchemaNode(JsonNode schemaNode, String path, List<ApiError.Detail> details) {
        JsonNode typeNode = schemaNode.get("type");
        if (typeNode != null && !isSupportedTypeNode(typeNode)) {
            details.add(ApiError.Detail.of(path + ".type", "unsupported_type", "Schema type 只能使用 V1 支持的基础类型。"));
        }
        JsonNode propertiesNode = schemaNode.get("properties");
        if (propertiesNode != null) {
            if (!propertiesNode.isObject()) {
                details.add(ApiError.Detail.of(path + ".properties", "type", "properties 必须是对象。"));
            } else {
                propertiesNode.properties().forEach(entry -> {
                    if (!entry.getValue().isObject()) {
                        details.add(ApiError.Detail.of(path + ".properties." + entry.getKey(), "type", "字段 Schema 必须是对象。"));
                    } else {
                        validateSchemaNode(entry.getValue(), path + ".properties." + entry.getKey(), details);
                    }
                });
            }
        }
        JsonNode requiredNode = schemaNode.get("required");
        if (requiredNode != null && !isStringArray(requiredNode)) {
            details.add(ApiError.Detail.of(path + ".required", "type", "required 必须是字符串数组。"));
        }
        JsonNode itemsNode = schemaNode.get("items");
        if (itemsNode != null) {
            if (!itemsNode.isObject()) {
                details.add(ApiError.Detail.of(path + ".items", "type", "items 必须是对象。"));
            } else {
                validateSchemaNode(itemsNode, path + ".items", details);
            }
        }
        JsonNode enumNode = schemaNode.get("enum");
        if (enumNode != null && !enumNode.isArray()) {
            details.add(ApiError.Detail.of(path + ".enum", "type", "enum 必须是数组。"));
        }
    }

    /**
     * 判断 type 节点是否为支持的类型声明。
     *
     * @param typeNode type 节点
     * @return 支持时返回 true
     */
    private boolean isSupportedTypeNode(JsonNode typeNode) {
        if (typeNode.isTextual()) {
            return SUPPORTED_TYPES.contains(typeNode.asText());
        }
        if (!typeNode.isArray()) {
            return false;
        }
        for (JsonNode item : typeNode) {
            if (!item.isTextual() || !SUPPORTED_TYPES.contains(item.asText())) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断节点是否为字符串数组。
     *
     * @param node 待判断节点
     * @return 是字符串数组时返回 true
     */
    private boolean isStringArray(JsonNode node) {
        if (!node.isArray()) {
            return false;
        }
        for (JsonNode item : node) {
            if (!item.isTextual()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 构造 Schema 定义非法异常。
     *
     * @param details 错误明细
     * @return 业务异常
     */
    private BizException invalid(List<ApiError.Detail> details) {
        return new BizException(ErrorCode.SCHEMA_VALIDATION_FAILED, "JSON Schema 定义校验失败。", details);
    }
}
