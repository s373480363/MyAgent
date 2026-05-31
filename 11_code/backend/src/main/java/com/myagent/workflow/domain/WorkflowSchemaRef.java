package com.myagent.workflow.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 工作流节点上的 Schema 引用。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "WorkflowSchemaRef", description = "工作流节点上的 Schema 引用。")
public final class WorkflowSchemaRef {

    /**
     * Schema 业务键。
     */
    @Schema(description = "Schema 业务键。", example = "summary-output", requiredMode = Schema.RequiredMode.REQUIRED)
    private String schemaKey;

    /**
     * Schema 版本号。
     */
    @Schema(description = "Schema 版本号。", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer version;

    public String getSchemaKey() {
        return schemaKey;
    }

    public void setSchemaKey(String schemaKey) {
        this.schemaKey = schemaKey;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
