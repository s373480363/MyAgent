package com.myagent.schema.application.command;

import com.fasterxml.jackson.databind.JsonNode;
import com.myagent.schema.domain.SchemaCreatedFrom;

/**
 * 创建 Schema 命令。
 *
 * @param schemaKey 稳定业务标识
 * @param name 中文名称
 * @param description 描述
 * @param jsonSchema JSON Schema 内容
 * @param javaType Java 类型
 * @param createdFrom 来源
 * @param sourceSchemaId 来源 Schema 主键
 */
public record CreateSchemaCommand(
        String schemaKey,
        String name,
        String description,
        JsonNode jsonSchema,
        String javaType,
        SchemaCreatedFrom createdFrom,
        Long sourceSchemaId
) {
}
