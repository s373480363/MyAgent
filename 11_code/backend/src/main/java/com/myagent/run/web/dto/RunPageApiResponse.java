package com.myagent.run.web.dto;

import com.myagent.common.api.ApiError;
import com.myagent.common.api.PageResponse;
import com.myagent.run.application.result.RunListItemResult;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 运行列表统一响应。
 */
@Schema(name = "RunPageApiResponse", description = "运行列表统一响应。")
public final class RunPageApiResponse {

    /**
     * 成功标记。
     */
    @Schema(description = "业务是否成功。", requiredMode = Schema.RequiredMode.REQUIRED)
    private final boolean success;

    /**
     * 分页数据。
     */
    @Schema(description = "运行分页列表。")
    private final PageResponse<RunListItemResult> data;

    /**
     * 错误对象。
     */
    @Schema(description = "错误对象。")
    private final ApiError error;

    /**
     * 构造统一响应。
     *
     * @param success 成功标记
     * @param data 分页数据
     * @param error 错误对象
     */
    public RunPageApiResponse(boolean success, PageResponse<RunListItemResult> data, ApiError error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    /**
     * 返回成功标记。
     *
     * @return 成功标记
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * 返回分页数据。
     *
     * @return 分页数据
     */
    public PageResponse<RunListItemResult> getData() {
        return data;
    }

    /**
     * 返回错误对象。
     *
     * @return 错误对象
     */
    public ApiError getError() {
        return error;
    }
}
