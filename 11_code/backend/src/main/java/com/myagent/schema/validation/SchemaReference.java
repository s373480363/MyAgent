package com.myagent.schema.validation;

/**
 * Schema 运行时引用。
 *
 * <p>运行时可以通过 schemaId 精确定位版本，也可以通过 schemaKey + version 回放历史语义。</p>
 */
public final class SchemaReference {

    /**
     * Schema 主键。
     */
    private final Long schemaId;

    /**
     * Schema 业务标识。
     */
    private final String schemaKey;

    /**
     * Schema 版本。
     */
    private final Integer version;

    /**
     * 构造 Schema 引用。
     *
     * @param schemaId Schema 主键
     * @param schemaKey Schema 业务标识
     * @param version Schema 版本
     */
    private SchemaReference(Long schemaId, String schemaKey, Integer version) {
        this.schemaId = schemaId;
        this.schemaKey = schemaKey;
        this.version = version;
    }

    /**
     * 使用主键构造 Schema 引用。
     *
     * @param schemaId Schema 主键
     * @return Schema 引用
     */
    public static SchemaReference byId(long schemaId) {
        return new SchemaReference(schemaId, null, null);
    }

    /**
     * 使用业务标识和版本构造 Schema 引用。
     *
     * @param schemaKey Schema 业务标识
     * @param version Schema 版本
     * @return Schema 引用
     */
    public static SchemaReference byKeyAndVersion(String schemaKey, int version) {
        return new SchemaReference(null, schemaKey, version);
    }

    /**
     * 返回 Schema 主键。
     *
     * @return Schema 主键
     */
    public Long getSchemaId() {
        return schemaId;
    }

    /**
     * 返回 Schema 业务标识。
     *
     * @return Schema 业务标识
     */
    public String getSchemaKey() {
        return schemaKey;
    }

    /**
     * 返回 Schema 版本。
     *
     * @return Schema 版本
     */
    public Integer getVersion() {
        return version;
    }
}
