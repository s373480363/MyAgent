package com.myagent.schema.application.result;

import com.fasterxml.jackson.databind.JsonNode;
import com.myagent.schema.domain.SchemaCreatedFrom;
import com.myagent.schema.domain.SchemaStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Schema 列表项。
 */
@Schema(name = "SchemaListItem", description = "Schema 列表项。")
public final class SchemaListItemResult {

    @Schema(description = "Schema 主键。", requiredMode = Schema.RequiredMode.REQUIRED)
    private final long id;

    @Schema(description = "稳定业务标识。", requiredMode = Schema.RequiredMode.REQUIRED)
    private final String schemaKey;

    @Schema(description = "整数版本号。", requiredMode = Schema.RequiredMode.REQUIRED)
    private final int version;

    @Schema(description = "中文名称。", requiredMode = Schema.RequiredMode.REQUIRED)
    private final String name;

    @Schema(description = "描述。")
    private final String description;

    @Schema(description = "JSON Schema 内容。")
    private final JsonNode jsonSchema;

    @Schema(description = "Java 类型。")
    private final String javaType;

    @Schema(description = "来源。", requiredMode = Schema.RequiredMode.REQUIRED)
    private final SchemaCreatedFrom createdFrom;

    @Schema(description = "生命周期状态。", requiredMode = Schema.RequiredMode.REQUIRED)
    private final SchemaStatus status;

    @Schema(description = "是否锁定。", requiredMode = Schema.RequiredMode.REQUIRED)
    private final boolean locked;

    @Schema(description = "创建时间。", requiredMode = Schema.RequiredMode.REQUIRED)
    private final Instant createdAt;

    @Schema(description = "更新时间。", requiredMode = Schema.RequiredMode.REQUIRED)
    private final Instant updatedAt;

    public SchemaListItemResult(
            long id,
            String schemaKey,
            int version,
            String name,
            String description,
            JsonNode jsonSchema,
            String javaType,
            SchemaCreatedFrom createdFrom,
            SchemaStatus status,
            boolean locked,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.schemaKey = schemaKey;
        this.version = version;
        this.name = name;
        this.description = description;
        this.jsonSchema = jsonSchema;
        this.javaType = javaType;
        this.createdFrom = createdFrom;
        this.status = status;
        this.locked = locked;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public long getId() {
        return id;
    }

    public String getSchemaKey() {
        return schemaKey;
    }

    public int getVersion() {
        return version;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public JsonNode getJsonSchema() {
        return jsonSchema;
    }

    public String getJavaType() {
        return javaType;
    }

    public SchemaCreatedFrom getCreatedFrom() {
        return createdFrom;
    }

    public SchemaStatus getStatus() {
        return status;
    }

    public boolean isLocked() {
        return locked;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
