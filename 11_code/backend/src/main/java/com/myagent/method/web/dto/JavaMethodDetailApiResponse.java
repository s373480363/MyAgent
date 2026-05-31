package com.myagent.method.web.dto;

import com.myagent.common.api.ApiError;
import com.myagent.method.application.result.JavaMethodDetailResult;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Java 方法详情统一响应契约。
 */
@Schema(name = "JavaMethodDetailApiResponse", description = "Java 方法详情统一响应。")
public final class JavaMethodDetailApiResponse {

    @Schema(description = "业务是否成功。", requiredMode = Schema.RequiredMode.REQUIRED)
    private final boolean success;

    @Schema(description = "Java 方法详情。")
    private final JavaMethodDetailResult data;

    @Schema(description = "错误对象。")
    private final ApiError error;

    /**
     * 构造详情响应。
     *
     * @param success 业务是否成功
     * @param data 详情数据
     * @param error 错误对象
     */
    public JavaMethodDetailApiResponse(boolean success, JavaMethodDetailResult data, ApiError error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    public boolean isSuccess() {
        return success;
    }

    public JavaMethodDetailResult getData() {
        return data;
    }

    public ApiError getError() {
        return error;
    }
}
