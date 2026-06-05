package com.myagent.schema.web.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.myagent.schema.domain.SchemaCreatedFrom;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 创建 Schema 请求。
 */
@Schema(name = "CreateSchemaRequest", description = "创建 Schema 请求。")
public final class CreateSchemaRequest {

    @NotBlank(message = "Schema Key 不能为空。")
    @Size(max = 128, message = "Schema Key 不能超过 128 个字符。")
    @Schema(description = "稳定业务标识。", example = "agent.input.summary", requiredMode = Schema.RequiredMode.REQUIRED)
    private String schemaKey;

    @NotBlank(message = "Schema 名称不能为空。")
    @Size(max = 200, message = "Schema 名称不能超过 200 个字符。")
    @Schema(description = "中文名称。", example = "摘要输入", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "描述。", example = "摘要 Agent 的输入结构", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String description;

    @NotNull(message = "JSON Schema 不能为空。")
    @Schema(description = "JSON Schema 内容。", requiredMode = Schema.RequiredMode.REQUIRED)
    private JsonNode jsonSchema;

    @Size(max = 255, message = "Java 类型不能超过 255 个字符。")
    @Schema(description = "Java 类型。", example = "syc.agentstudio.example.SummaryInput", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String javaType;

    @NotNull(message = "Schema 来源不能为空。")
    @Schema(description = "Schema 来源。", requiredMode = Schema.RequiredMode.REQUIRED)
    private SchemaCreatedFrom createdFrom;

    @Schema(description = "来源 Schema 主键。", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Long sourceSchemaId;

    public String getSchemaKey() {
        return schemaKey;
    }

    public void setSchemaKey(String schemaKey) {
        this.schemaKey = schemaKey;
    }

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

    public SchemaCreatedFrom getCreatedFrom() {
        return createdFrom;
    }

    public void setCreatedFrom(SchemaCreatedFrom createdFrom) {
        this.createdFrom = createdFrom;
    }

    public Long getSourceSchemaId() {
        return sourceSchemaId;
    }

    public void setSourceSchemaId(Long sourceSchemaId) {
        this.sourceSchemaId = sourceSchemaId;
    }
}
