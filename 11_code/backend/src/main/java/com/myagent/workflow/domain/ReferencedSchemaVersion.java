package com.myagent.workflow.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 工作流版本引用的 Schema 快照。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "ReferencedSchemaVersion", description = "工作流版本引用的 Schema 快照。")
public final class ReferencedSchemaVersion {

    /**
     * Schema 主键。
     */
    @Schema(description = "Schema 主键。", example = "12", requiredMode = Schema.RequiredMode.REQUIRED)
    private final long schemaId;

    /**
     * Schema 业务键。
     */
    @Schema(description = "Schema 业务键。", example = "summary-output", requiredMode = Schema.RequiredMode.REQUIRED)
    private final String schemaKey;

    /**
     * Schema 版本号。
     */
    @Schema(description = "Schema 版本号。", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
    private final int version;

    /**
     * 构造引用快照。
     *
     * @param schemaId Schema 主键
     * @param schemaKey Schema 业务键
     * @param version Schema 版本号
     */
    @JsonCreator
    public ReferencedSchemaVersion(
            @JsonProperty(value = "schemaId", required = true) long schemaId,
            @JsonProperty(value = "schemaKey", required = true) String schemaKey,
            @JsonProperty(value = "version", required = true) int version
    ) {
        this.schemaId = schemaId;
        this.schemaKey = schemaKey;
        this.version = version;
    }

    /**
     * 返回 Schema 主键。
     *
     * @return Schema 主键
     */
    public long getSchemaId() {
        return schemaId;
    }

    /**
     * 返回 Schema 业务键。
     *
     * @return Schema 业务键
     */
    public String getSchemaKey() {
        return schemaKey;
    }

    /**
     * 返回 Schema 版本号。
     *
     * @return Schema 版本号
     */
    public int getVersion() {
        return version;
    }
}
