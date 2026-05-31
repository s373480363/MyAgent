package com.myagent.externalagent.web.dto;

import com.myagent.common.api.ApiError;
import com.myagent.externalagent.application.result.ExternalAgentDetailResult;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 外部 Agent 详情统一响应契约。
 */
@Schema(name = "ExternalAgentDetailApiResponse", description = "外部 Agent 详情统一响应。")
public final class ExternalAgentDetailApiResponse {

    @Schema(description = "业务是否成功。", requiredMode = Schema.RequiredMode.REQUIRED)
    private final boolean success;

    @Schema(description = "外部 Agent 详情。")
    private final ExternalAgentDetailResult data;

    @Schema(description = "错误对象。")
    private final ApiError error;

    /**
     * 构造详情响应。
     *
     * @param success 业务是否成功
     * @param data 详情数据
     * @param error 错误对象
     */
    public ExternalAgentDetailApiResponse(boolean success, ExternalAgentDetailResult data, ApiError error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    public boolean isSuccess() {
        return success;
    }

    public ExternalAgentDetailResult getData() {
        return data;
    }

    public ApiError getError() {
        return error;
    }
}
