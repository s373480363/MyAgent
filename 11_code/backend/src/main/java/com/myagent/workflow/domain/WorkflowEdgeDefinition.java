package com.myagent.workflow.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 工作流边定义。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "WorkflowEdgeDefinition", description = "工作流边定义。")
public final class WorkflowEdgeDefinition {

    /**
     * 边标识。
     */
    @Schema(description = "边标识，在版本内唯一。", example = "edge-1", requiredMode = Schema.RequiredMode.REQUIRED)
    private String edgeId;

    /**
     * 源节点标识。
     */
    @Schema(description = "源节点标识。", example = "node-start", requiredMode = Schema.RequiredMode.REQUIRED)
    private String sourceNodeId;

    /**
     * 目标节点标识。
     */
    @Schema(description = "目标节点标识。", example = "node-end", requiredMode = Schema.RequiredMode.REQUIRED)
    private String targetNodeId;

    /**
     * 边类型。
     */
    @Schema(description = "边类型。", requiredMode = Schema.RequiredMode.REQUIRED)
    private WorkflowEdgeType type;

    /**
     * 条件对象。
     */
    @Schema(description = "条件对象。")
    private JsonNode condition;

    /**
     * 是否默认分支。
     */
    @Schema(description = "是否默认分支。", example = "false")
    private Boolean isDefault;

    /**
     * 边说明。
     */
    @Schema(description = "边说明。")
    private String description;

    public String getEdgeId() {
        return edgeId;
    }

    public void setEdgeId(String edgeId) {
        this.edgeId = edgeId;
    }

    public String getSourceNodeId() {
        return sourceNodeId;
    }

    public void setSourceNodeId(String sourceNodeId) {
        this.sourceNodeId = sourceNodeId;
    }

    public String getTargetNodeId() {
        return targetNodeId;
    }

    public void setTargetNodeId(String targetNodeId) {
        this.targetNodeId = targetNodeId;
    }

    public WorkflowEdgeType getType() {
        return type;
    }

    public void setType(WorkflowEdgeType type) {
        this.type = type;
    }

    public JsonNode getCondition() {
        return condition;
    }

    public void setCondition(JsonNode condition) {
        this.condition = condition;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean aDefault) {
        isDefault = aDefault;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
