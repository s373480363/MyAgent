package com.myagent.settings.web.dto;

import com.myagent.common.api.ApiError;
import com.myagent.settings.application.result.SettingItemResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 系统设置列表统一响应契约。
 */
@Schema(name = "SettingsListApiResponse", description = "系统设置列表统一响应。")
public final class SettingsListApiResponse {

    /**
     * 业务是否成功。
     */
    @Schema(description = "业务是否成功。", requiredMode = Schema.RequiredMode.REQUIRED)
    private final boolean success;

    /**
     * 系统设置列表。
     */
    @Schema(description = "系统设置列表。")
    private final List<SettingItemResult> data;

    /**
     * 错误对象。
     */
    @Schema(description = "错误对象。")
    private final ApiError error;

    /**
     * 构造系统设置列表响应。
     *
     * @param success 业务是否成功
     * @param data 系统设置列表
     * @param error 错误对象
     */
    public SettingsListApiResponse(boolean success, List<SettingItemResult> data, ApiError error) {
        this.success = success;
        this.data = data == null ? List.of() : List.copyOf(data);
        this.error = error;
    }

    /**
     * 返回业务是否成功。
     *
     * @return 业务是否成功
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * 返回系统设置列表。
     *
     * @return 系统设置列表
     */
    public List<SettingItemResult> getData() {
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
