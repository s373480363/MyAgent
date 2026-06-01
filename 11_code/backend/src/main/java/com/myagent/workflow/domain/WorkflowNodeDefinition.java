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

    /**
     * 返回节点标识。
     *
     * @return 节点标识
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * 设置节点标识。
     *
     * @param nodeId 节点标识
     */
    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    /**
     * 返回节点类型。
     *
     * @return 节点类型
     */
    public WorkflowNodeType getType() {
        return type;
    }

    /**
     * 设置节点类型。
     *
     * @param type 节点类型
     */
    public void setType(WorkflowNodeType type) {
        this.type = type;
    }

    /**
     * 返回节点名称。
     *
     * @return 节点名称
     */
    public String getName() {
        return name;
    }

    /**
     * 设置节点名称。
     *
     * @param name 节点名称
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 返回节点说明。
     *
     * @return 节点说明
     */
    public String getDescription() {
        return description;
    }

    /**
     * 设置节点说明。
     *
     * @param description 节点说明
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 返回输入 Schema 引用。
     *
     * @return 输入 Schema 引用
     */
    public WorkflowSchemaRef getInputSchemaRef() {
        return inputSchemaRef;
    }

    /**
     * 设置输入 Schema 引用。
     *
     * @param inputSchemaRef 输入 Schema 引用
     */
    public void setInputSchemaRef(WorkflowSchemaRef inputSchemaRef) {
        this.inputSchemaRef = inputSchemaRef;
    }

    /**
     * 返回输出 Schema 引用。
     *
     * @return 输出 Schema 引用
     */
    public WorkflowSchemaRef getOutputSchemaRef() {
        return outputSchemaRef;
    }

    /**
     * 设置输出 Schema 引用。
     *
     * @param outputSchemaRef 输出 Schema 引用
     */
    public void setOutputSchemaRef(WorkflowSchemaRef outputSchemaRef) {
        this.outputSchemaRef = outputSchemaRef;
    }

    /**
     * 返回输入映射。
     *
     * @return 输入映射
     */
    public JsonNode getInputMapping() {
        return inputMapping;
    }

    /**
     * 设置输入映射。
     *
     * @param inputMapping 输入映射
     */
    public void setInputMapping(JsonNode inputMapping) {
        this.inputMapping = inputMapping;
    }

    /**
     * 返回输出映射。
     *
     * @return 输出映射
     */
    public JsonNode getOutputMapping() {
        return outputMapping;
    }

    /**
     * 设置输出映射。
     *
     * @param outputMapping 输出映射
     */
    public void setOutputMapping(JsonNode outputMapping) {
        this.outputMapping = outputMapping;
    }

    /**
     * 返回节点级超时秒数。
     *
     * @return 节点级超时秒数
     */
    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    /**
     * 设置节点级超时秒数。
     *
     * @param timeoutSeconds 节点级超时秒数
     */
    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    /**
     * 返回失败策略。
     *
     * @return 失败策略
     */
    public String getFailurePolicy() {
        return failurePolicy;
    }

    /**
     * 设置失败策略。
     *
     * @param failurePolicy 失败策略
     */
    public void setFailurePolicy(String failurePolicy) {
        this.failurePolicy = failurePolicy;
    }

    /**
     * 返回节点专属配置。
     *
     * @return 节点专属配置
     */
    public JsonNode getConfig() {
        return config;
    }

    /**
     * 设置节点专属配置。
     *
     * @param config 节点专属配置
     */
    public void setConfig(JsonNode config) {
        this.config = config;
    }

    /**
     * 返回画布展示信息。
     *
     * @return 画布展示信息
     */
    public WorkflowNodeUi getUi() {
        return ui;
    }

    /**
     * 设置画布展示信息。
     *
     * @param ui 画布展示信息
     */
    public void setUi(WorkflowNodeUi ui) {
        this.ui = ui;
    }
}
