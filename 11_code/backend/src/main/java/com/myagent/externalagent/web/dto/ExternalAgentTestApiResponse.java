package com.myagent.externalagent.web.dto;

import com.myagent.common.api.ApiError;
import com.myagent.externalagent.application.result.ExternalAgentTestResult;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 外部 Agent 测试统一响应契约。
 */
@Schema(name = "ExternalAgentTestApiResponse", description = "外部 Agent 测试统一响应。")
public final class ExternalAgentTestApiResponse {

    @Schema(description = "业务是否成功。", requiredMode = Schema.RequiredMode.REQUIRED)
    private final boolean success;

    @Schema(description = "测试结果。")
    private final ExternalAgentTestResult data;

    @Schema(description = "错误对象。")
    private final ApiError error;

    /**
     * 构造测试响应。
     *
     * @param success 业务是否成功
     * @param data 测试结果
     * @param error 错误对象
     */
    public ExternalAgentTestApiResponse(boolean success, ExternalAgentTestResult data, ApiError error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    public boolean isSuccess() {
        return success;
    }

    public ExternalAgentTestResult getData() {
        return data;
    }

    public ApiError getError() {
        return error;
    }
}
