package com.myagent.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 统一业务响应包装。
 *
 * @param <T> 业务数据类型
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "ApiResponse", description = "统一业务响应包装。")
public final class ApiResponse<T> {

    /**
     * 业务是否成功。
     */
    @Schema(description = "业务是否成功。", requiredMode = Schema.RequiredMode.REQUIRED)
    private final boolean success;

    /**
     * 成功时返回的业务数据。
     */
    @Schema(description = "成功时返回的业务数据。")
    private final T data;

    /**
     * 失败时返回的错误对象。
     */
    @Schema(description = "失败时返回的错误对象。")
    private final ApiError error;

    /**
     * 构造统一响应包装。
     *
     * @param success 业务是否成功
     * @param data 成功时的数据
     * @param error 失败时的错误对象
     */
    public ApiResponse(boolean success, T data, ApiError error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    /**
     * 构造成功响应。
     *
     * @param data 业务数据
     * @param <T> 业务数据类型
     * @return 成功响应
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    /**
     * 构造失败响应。
     *
     * @param error 错误对象
     * @return 失败响应
     */
    public static ApiResponse<Void> failure(ApiError error) {
        return new ApiResponse<>(false, null, error);
    }

    /**
     * 返回业务是否成功。
     *
     * @return 成功标记
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * 返回业务数据。
     *
     * @return 业务数据
     */
    public T getData() {
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
