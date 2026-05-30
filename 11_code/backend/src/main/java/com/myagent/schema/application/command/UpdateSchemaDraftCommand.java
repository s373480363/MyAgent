package com.myagent.schema.application.command;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 更新 Schema 草稿命令。
 *
 * @param schemaId Schema 主键
 * @param name 中文名称
 * @param description 描述
 * @param jsonSchema JSON Schema 内容
 * @param javaType Java 类型
 */
public record UpdateSchemaDraftCommand(
        long schemaId,
        String name,
        String description,
        JsonNode jsonSchema,
        String javaType
) {
}
