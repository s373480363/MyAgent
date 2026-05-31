package com.myagent.workflow.web.dto;

import com.myagent.common.api.ApiError;
import com.myagent.workflow.application.result.WorkflowValidationResult;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 工作流校验统一响应。
 */
@Schema(name = "WorkflowValidationApiResponse", description = "工作流校验统一响应。")
public final class WorkflowValidationApiResponse {

    @Schema(description = "业务是否成功。", requiredMode = Schema.RequiredMode.REQUIRED)
    private final boolean success;

    @Schema(description = "工作流校验结果。")
    private final WorkflowValidationResult data;

    @Schema(description = "错误对象。")
    private final ApiError error;

    /**
     * 构造统一响应。
     *
     * @param success 成功标记
     * @param data 校验结果
     * @param error 错误对象
     */
    public WorkflowValidationApiResponse(boolean success, WorkflowValidationResult data, ApiError error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    public boolean isSuccess() {
        return success;
    }

    public WorkflowValidationResult getData() {
        return data;
    }

    public ApiError getError() {
        return error;
    }
}
