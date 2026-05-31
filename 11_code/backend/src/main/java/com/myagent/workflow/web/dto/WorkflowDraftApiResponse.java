package com.myagent.workflow.web.dto;

import com.myagent.common.api.ApiError;
import com.myagent.workflow.application.result.WorkflowDraftResult;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 工作流草稿统一响应。
 */
@Schema(name = "WorkflowDraftApiResponse", description = "工作流草稿统一响应。")
public final class WorkflowDraftApiResponse {

    @Schema(description = "业务是否成功。", requiredMode = Schema.RequiredMode.REQUIRED)
    private final boolean success;

    @Schema(description = "工作流草稿数据。")
    private final WorkflowDraftResult data;

    @Schema(description = "错误对象。")
    private final ApiError error;

    /**
     * 构造统一响应。
     *
     * @param success 成功标记
     * @param data 草稿数据
     * @param error 错误对象
     */
    public WorkflowDraftApiResponse(boolean success, WorkflowDraftResult data, ApiError error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    public boolean isSuccess() {
        return success;
    }

    public WorkflowDraftResult getData() {
        return data;
    }

    public ApiError getError() {
        return error;
    }
}
