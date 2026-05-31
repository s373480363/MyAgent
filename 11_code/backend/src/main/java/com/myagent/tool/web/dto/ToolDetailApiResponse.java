package com.myagent.tool.web.dto;

import com.myagent.common.api.ApiError;
import com.myagent.tool.application.result.ToolDetailResult;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 工具详情统一响应契约。
 */
@Schema(name = "ToolDetailApiResponse", description = "工具详情统一响应。")
public final class ToolDetailApiResponse {

    @Schema(description = "业务是否成功。", requiredMode = Schema.RequiredMode.REQUIRED)
    private final boolean success;

    @Schema(description = "工具详情。")
    private final ToolDetailResult data;

    @Schema(description = "错误对象。")
    private final ApiError error;

    /**
     * 构造详情响应。
     *
     * @param success 业务是否成功
     * @param data 详情数据
     * @param error 错误对象
     */
    public ToolDetailApiResponse(boolean success, ToolDetailResult data, ApiError error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    public boolean isSuccess() {
        return success;
    }

    public ToolDetailResult getData() {
        return data;
    }

    public ApiError getError() {
        return error;
    }
}
