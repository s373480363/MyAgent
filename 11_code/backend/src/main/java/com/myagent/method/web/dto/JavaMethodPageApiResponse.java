package com.myagent.method.web.dto;

import com.myagent.common.api.ApiError;
import com.myagent.common.api.PageResponse;
import com.myagent.method.application.result.JavaMethodListItemResult;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Java 方法分页统一响应契约。
 */
@Schema(name = "JavaMethodPageApiResponse", description = "Java 方法分页统一响应。")
public final class JavaMethodPageApiResponse {

    @Schema(description = "业务是否成功。", requiredMode = Schema.RequiredMode.REQUIRED)
    private final boolean success;

    @Schema(description = "Java 方法分页数据。")
    private final PageResponse<JavaMethodListItemResult> data;

    @Schema(description = "错误对象。")
    private final ApiError error;

    /**
     * 构造分页响应。
     *
     * @param success 业务是否成功
     * @param data 分页数据
     * @param error 错误对象
     */
    public JavaMethodPageApiResponse(boolean success, PageResponse<JavaMethodListItemResult> data, ApiError error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    public boolean isSuccess() {
        return success;
    }

    public PageResponse<JavaMethodListItemResult> getData() {
        return data;
    }

    public ApiError getError() {
        return error;
    }
}
