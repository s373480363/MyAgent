package com.myagent.workflow.web.dto;

import com.myagent.common.api.ApiError;
import com.myagent.workflow.application.result.WorkflowPublishResult;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 工作流发布统一响应。
 */
@Schema(name = "WorkflowPublishApiResponse", description = "工作流发布统一响应。")
public final class WorkflowPublishApiResponse {

    @Schema(description = "业务是否成功。", requiredMode = Schema.RequiredMode.REQUIRED)
    private final boolean success;

    @Schema(description = "工作流发布结果。")
    private final WorkflowPublishResult data;

    @Schema(description = "错误对象。")
    private final ApiError error;

    /**
     * 构造统一响应。
     *
     * @param success 成功标记
     * @param data 发布结果
     * @param error 错误对象
     */
    public WorkflowPublishApiResponse(boolean success, WorkflowPublishResult data, ApiError error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    public boolean isSuccess() {
        return success;
    }

    public WorkflowPublishResult getData() {
        return data;
    }

    public ApiError getError() {
        return error;
    }
}
