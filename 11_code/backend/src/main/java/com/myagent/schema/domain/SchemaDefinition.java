package com.myagent.schema.domain;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

/**
 * SchemaDefinition 领域对象。
 *
 * <p>SchemaDefinition 是平台唯一跨层结构契约，负责承载结构版本、锁定状态和 JSON Schema 内容。</p>
 */
public final class SchemaDefinition {

    /**
     * 内部主键。
     */
    private final long id;

    /**
     * 稳定业务标识。
     */
    private final String schemaKey;

    /**
     * 整数版本号。
     */
    private final int version;

    /**
     * 中文名称。
     */
    private final String name;

    /**
     * 描述。
     */
    private final String description;

    /**
     * JSON Schema 内容。
     */
    private final JsonNode jsonSchema;

    /**
     * 可选 Java 类型。
     */
    private final String javaType;

    /**
     * 来源。
     */
    private final SchemaCreatedFrom createdFrom;

    /**
     * 生命周期状态。
     */
    private final SchemaStatus status;

    /**
     * 是否被发布工作流引用锁定。
     */
    private final boolean locked;

    /**
     * 创建时间。
     */
    private final Instant createdAt;

    /**
     * 更新时间。
     */
    private final Instant updatedAt;

    /**
     * 构造 SchemaDefinition。
     *
     * @param id 内部主键
     * @param schemaKey 稳定业务标识
     * @param version 整数版本号
     * @param name 中文名称
     * @param description 描述
     * @param jsonSchema JSON Schema 内容
     * @param javaType 可选 Java 类型
     * @param createdFrom 来源
     * @param status 生命周期状态
     * @param locked 是否锁定
     * @param createdAt 创建时间
     * @param updatedAt 更新时间
     */
    public SchemaDefinition(
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

    /**
     * 判断当前版本是否允许原地更新。
     *
     * @return 允许更新时返回 true
     */
    public boolean isEditableDraft() {
        return status == SchemaStatus.DRAFT && !locked;
    }

    /**
     * 返回内部主键。
     *
     * @return 内部主键
     */
    public long getId() {
        return id;
    }

    /**
     * 返回稳定业务标识。
     *
     * @return 稳定业务标识
     */
    public String getSchemaKey() {
        return schemaKey;
    }

    /**
     * 返回整数版本号。
     *
     * @return 整数版本号
     */
    public int getVersion() {
        return version;
    }

    /**
     * 返回中文名称。
     *
     * @return 中文名称
     */
    public String getName() {
        return name;
    }

    /**
     * 返回描述。
     *
     * @return 描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 返回 JSON Schema 内容。
     *
     * @return JSON Schema 内容
     */
    public JsonNode getJsonSchema() {
        return jsonSchema;
    }

    /**
     * 返回可选 Java 类型。
     *
     * @return 可选 Java 类型
     */
    public String getJavaType() {
        return javaType;
    }

    /**
     * 返回来源。
     *
     * @return 来源
     */
    public SchemaCreatedFrom getCreatedFrom() {
        return createdFrom;
    }

    /**
     * 返回生命周期状态。
     *
     * @return 生命周期状态
     */
    public SchemaStatus getStatus() {
        return status;
    }

    /**
     * 返回是否锁定。
     *
     * @return 是否锁定
     */
    public boolean isLocked() {
        return locked;
    }

    /**
     * 返回创建时间。
     *
     * @return 创建时间
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * 返回更新时间。
     *
     * @return 更新时间
     */
    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
