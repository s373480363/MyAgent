package com.myagent.run.web.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 调试运行请求。
 */
@Schema(name = "DebugRunRequest", description = "调试运行请求。")
public final class DebugRunRequest {

    /**
     * 工作流版本主键。
     */
    @Schema(description = "工作流版本主键。")
    private Long workflowVersionId;

    /**
     * 运行输入。
     */
    @Schema(description = "运行输入。")
    private JsonNode input;

    /**
     * 返回工作流版本主键。
     *
     * @return 工作流版本主键
     */
    public Long getWorkflowVersionId() {
        return workflowVersionId;
    }

    /**
     * 设置工作流版本主键。
     *
     * @param workflowVersionId 工作流版本主键
     */
    public void setWorkflowVersionId(Long workflowVersionId) {
        this.workflowVersionId = workflowVersionId;
    }

    /**
     * 返回运行输入。
     *
     * @return 运行输入
     */
    public JsonNode getInput() {
        return input;
    }

    /**
     * 设置运行输入。
     *
     * @param input 运行输入
     */
    public void setInput(JsonNode input) {
        this.input = input;
    }
}
