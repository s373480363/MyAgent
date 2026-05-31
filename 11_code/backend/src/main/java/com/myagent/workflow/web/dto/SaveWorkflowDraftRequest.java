package com.myagent.workflow.web.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.myagent.workflow.domain.WorkflowEdgeDefinition;
import com.myagent.workflow.domain.WorkflowNodeDefinition;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 保存工作流草稿请求。
 */
public final class SaveWorkflowDraftRequest {

    /**
     * 节点定义列表。
     */
    @Valid
    @NotNull(message = "nodes 不能为空。")
    private List<WorkflowNodeDefinition> nodes;

    /**
     * 边定义列表。
     */
    @Valid
    @NotNull(message = "edges 不能为空。")
    private List<WorkflowEdgeDefinition> edges;

    /**
     * 原始运行约束对象。
     */
    private JsonNode runtimeOptions;

    public List<WorkflowNodeDefinition> getNodes() {
        return nodes;
    }

    public void setNodes(List<WorkflowNodeDefinition> nodes) {
        this.nodes = nodes;
    }

    public List<WorkflowEdgeDefinition> getEdges() {
        return edges;
    }

    public void setEdges(List<WorkflowEdgeDefinition> edges) {
        this.edges = edges;
    }

    public JsonNode getRuntimeOptions() {
        return runtimeOptions;
    }

    public void setRuntimeOptions(JsonNode runtimeOptions) {
        this.runtimeOptions = runtimeOptions;
    }
}
