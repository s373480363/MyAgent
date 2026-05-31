package com.myagent.workflow.web.dto;

import com.myagent.common.api.ApiError;
import com.myagent.common.api.PageResponse;
import com.myagent.workflow.application.result.WorkflowVersionListItemResult;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 工作流版本分页统一响应。
 */
@Schema(name = "WorkflowVersionPageApiResponse", description = "工作流版本分页统一响应。")
public final class WorkflowVersionPageApiResponse {

    @Schema(description = "业务是否成功。", requiredMode = Schema.RequiredMode.REQUIRED)
    private final boolean success;

    @Schema(description = "工作流版本分页数据。")
    private final PageResponse<WorkflowVersionListItemResult> data;

    @Schema(description = "错误对象。")
    private final ApiError error;

    /**
     * 构造统一响应。
     *
     * @param success 成功标记
     * @param data 分页数据
     * @param error 错误对象
     */
    public WorkflowVersionPageApiResponse(
            boolean success,
            PageResponse<WorkflowVersionListItemResult> data,
            ApiError error
    ) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    public boolean isSuccess() {
        return success;
    }

    public PageResponse<WorkflowVersionListItemResult> getData() {
        return data;
    }

    public ApiError getError() {
        return error;
    }
}
