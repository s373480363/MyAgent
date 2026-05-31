package com.myagent.workflow.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 工作流节点定义。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "WorkflowNodeDefinition", description = "工作流节点定义。")
public final class WorkflowNodeDefinition {

    /**
     * 节点标识。
     */
    @Schema(description = "节点内部标识，在同一工作流版本内唯一。", example = "node-start", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nodeId;

    /**
     * 节点类型。
     */
    @Schema(description = "节点类型。", requiredMode = Schema.RequiredMode.REQUIRED)
    private WorkflowNodeType type;

    /**
     * 节点名称。
     */
    @Schema(description = "节点名称。", example = "开始节点", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    /**
     * 节点说明。
     */
    @Schema(description = "节点说明。")
    private String description;

    /**
     * 输入 Schema 引用。
     */
    @Schema(description = "输入 Schema 引用。")
    private WorkflowSchemaRef inputSchemaRef;

    /**
     * 输出 Schema 引用。
     */
    @Schema(description = "输出 Schema 引用。")
    private WorkflowSchemaRef outputSchemaRef;

    /**
     * 输入映射。
     */
    @Schema(description = "输入映射对象。")
    private JsonNode inputMapping;

    /**
     * 输出映射。
     */
    @Schema(description = "输出映射对象。")
    private JsonNode outputMapping;

    /**
     * 节点级超时。
     */
    @Schema(description = "节点级超时（秒）。", example = "120")
    private Integer timeoutSeconds;

    /**
     * 失败策略。
     */
    @Schema(description = "失败策略。", example = "FAIL_FAST")
    private String failurePolicy;

    /**
     * 节点专属配置。
     */
    @Schema(description = "节点专属配置。")
    private JsonNode config;

    /**
     * 画布展示信息。
     */
    @Schema(description = "画布展示信息。")
    private WorkflowNodeUi ui;

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public WorkflowNodeType getType() {
        return type;
    }

    public void setType(WorkflowNodeType type) {
        this.type = type;
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

    public WorkflowSchemaRef getInputSchemaRef() {
        return inputSchemaRef;
    }

    public void setInputSchemaRef(WorkflowSchemaRef inputSchemaRef) {
        this.inputSchemaRef = inputSchemaRef;
    }

    public WorkflowSchemaRef getOutputSchemaRef() {
        return outputSchemaRef;
    }

    public void setOutputSchemaRef(WorkflowSchemaRef outputSchemaRef) {
        this.outputSchemaRef = outputSchemaRef;
    }

    public JsonNode getInputMapping() {
        return inputMapping;
    }

    public void setInputMapping(JsonNode inputMapping) {
        this.inputMapping = inputMapping;
    }

    public JsonNode getOutputMapping() {
        return outputMapping;
    }

    public void setOutputMapping(JsonNode outputMapping) {
        this.outputMapping = outputMapping;
    }

    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public String getFailurePolicy() {
        return failurePolicy;
    }

    public void setFailurePolicy(String failurePolicy) {
        this.failurePolicy = failurePolicy;
    }

    public JsonNode getConfig() {
        return config;
    }

    public void setConfig(JsonNode config) {
        this.config = config;
    }

    public WorkflowNodeUi getUi() {
        return ui;
    }

    public void setUi(WorkflowNodeUi ui) {
        this.ui = ui;
    }
}
