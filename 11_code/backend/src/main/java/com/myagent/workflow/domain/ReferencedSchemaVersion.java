package com.myagent.workflow.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
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
    public ReferencedSchemaVersion(long schemaId, String schemaKey, int version) {
        this.schemaId = schemaId;
        this.schemaKey = schemaKey;
        this.version = version;
    }

    public long getSchemaId() {
        return schemaId;
    }

    public String getSchemaKey() {
        return schemaKey;
    }

    public int getVersion() {
        return version;
    }
}
