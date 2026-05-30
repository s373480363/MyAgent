package com.myagent.schema.application.command;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 基于历史版本创建新 Schema 版本命令。
 *
 * @param schemaId 来源 Schema 主键
 * @param name 中文名称
 * @param description 描述
 * @param jsonSchema JSON Schema 内容
 * @param javaType Java 类型
 */
public record CreateSchemaVersionCommand(
        long schemaId,
        String name,
        String description,
        JsonNode jsonSchema,
        String javaType
) {
}
