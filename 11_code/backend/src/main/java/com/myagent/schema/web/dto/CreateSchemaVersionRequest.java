package com.myagent.schema.web.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 创建 Schema 新版本请求。
 */
@Schema(name = "CreateSchemaVersionRequest", description = "创建 Schema 新版本请求。")
public final class CreateSchemaVersionRequest {

    @NotBlank(message = "Schema 名称不能为空。")
    @Size(max = 200, message = "Schema 名称不能超过 200 个字符。")
    @Schema(description = "中文名称。", example = "摘要输入", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "描述。", example = "摘要 Agent 的输入结构 v2", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String description;

    @NotNull(message = "JSON Schema 不能为空。")
    @Schema(description = "JSON Schema 内容。", requiredMode = Schema.RequiredMode.REQUIRED)
    private JsonNode jsonSchema;

    @Size(max = 255, message = "Java 类型不能超过 255 个字符。")
    @Schema(description = "Java 类型。", example = "com.myagent.schema.SummaryInput", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String javaType;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public JsonNode getJsonSchema() {
        return jsonSchema;
    }

    public void setJsonSchema(JsonNode jsonSchema) {
        this.jsonSchema = jsonSchema;
    }

    public String getJavaType() {
        return javaType;
    }

    public void setJavaType(String javaType) {
        this.javaType = javaType;
    }
}
