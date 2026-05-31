package com.myagent.tool.web.dto;

import com.myagent.common.api.ApiError;
import com.myagent.common.api.PageResponse;
import com.myagent.tool.application.result.ToolListItemResult;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 工具分页统一响应契约。
 */
@Schema(name = "ToolPageApiResponse", description = "工具分页统一响应。")
public final class ToolPageApiResponse {

    @Schema(description = "业务是否成功。", requiredMode = Schema.RequiredMode.REQUIRED)
    private final boolean success;

    @Schema(description = "工具分页数据。")
    private final PageResponse<ToolListItemResult> data;

    @Schema(description = "错误对象。")
    private final ApiError error;

    /**
     * 构造分页响应。
     *
     * @param success 业务是否成功
     * @param data 分页数据
     * @param error 错误对象
     */
    public ToolPageApiResponse(boolean success, PageResponse<ToolListItemResult> data, ApiError error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    public boolean isSuccess() {
        return success;
    }

    public PageResponse<ToolListItemResult> getData() {
        return data;
    }

    public ApiError getError() {
        return error;
    }
}
