package com.myagent.workflow.web.dto;

import com.myagent.common.api.ApiError;
import com.myagent.workflow.application.result.WorkflowVersionResult;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 工作流版本详情统一响应。
 */
@Schema(name = "WorkflowVersionDetailApiResponse", description = "工作流版本详情统一响应。")
public final class WorkflowVersionDetailApiResponse {

    @Schema(description = "业务是否成功。", requiredMode = Schema.RequiredMode.REQUIRED)
    private final boolean success;

    @Schema(description = "工作流版本详情。")
    private final WorkflowVersionResult data;

    @Schema(description = "错误对象。")
    private final ApiError error;

    /**
     * 构造统一响应。
     *
     * @param success 成功标记
     * @param data 版本详情
     * @param error 错误对象
     */
    public WorkflowVersionDetailApiResponse(boolean success, WorkflowVersionResult data, ApiError error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    public boolean isSuccess() {
        return success;
    }

    public WorkflowVersionResult getData() {
        return data;
    }

    public ApiError getError() {
        return error;
    }
}
