package com.myagent.schema.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.myagent.schema.domain.SchemaCreatedFrom;
import com.myagent.schema.domain.SchemaStatus;

import java.time.Instant;

/**
 * Schema 持久化记录。
 */
public final class SchemaRecord {

    private final long id;
    private final String schemaKey;
    private final int version;
    private final String name;
    private final String description;
    private final JsonNode jsonSchema;
    private final String javaType;
    private final SchemaCreatedFrom createdFrom;
    private final SchemaStatus status;
    private final boolean locked;
    private final Instant createdAt;
    private final Instant updatedAt;

    public SchemaRecord(
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
