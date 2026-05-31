package com.myagent.run.web.dto;

import com.myagent.common.api.ApiError;
import com.myagent.run.application.result.RunDetailResult;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 运行详情统一响应。
 */
@Schema(name = "RunDetailApiResponse", description = "运行详情统一响应。")
public final class RunDetailApiResponse {

    /**
     * 成功标记。
     */
    @Schema(description = "业务是否成功。", requiredMode = Schema.RequiredMode.REQUIRED)
    private final boolean success;

    /**
     * 运行详情。
     */
    @Schema(description = "运行详情。")
    private final RunDetailResult data;

    /**
     * 错误对象。
     */
    @Schema(description = "错误对象。")
    private final ApiError error;

    /**
     * 构造统一响应。
     *
     * @param success 成功标记
     * @param data 运行详情
     * @param error 错误对象
     */
    public RunDetailApiResponse(boolean success, RunDetailResult data, ApiError error) {
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
     * 返回运行详情。
     *
     * @return 运行详情
     */
    public RunDetailResult getData() {
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
