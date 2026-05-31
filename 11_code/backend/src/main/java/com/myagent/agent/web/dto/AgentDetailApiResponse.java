package com.myagent.agent.web.dto;

import com.myagent.agent.application.result.AgentDetailResult;
import com.myagent.common.api.ApiError;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Agent 详情统一响应。
 */
@Schema(name = "AgentDetailApiResponse", description = "Agent 详情统一响应。")
public final class AgentDetailApiResponse {

    @Schema(description = "业务是否成功。", requiredMode = Schema.RequiredMode.REQUIRED)
    private final boolean success;

    @Schema(description = "Agent 详情。")
    private final AgentDetailResult data;

    @Schema(description = "错误对象。")
    private final ApiError error;

    /**
     * 构造统一响应。
     *
     * @param success 成功标记
     * @param data Agent 详情
     * @param error 错误对象
     */
    public AgentDetailApiResponse(boolean success, AgentDetailResult data, ApiError error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    public boolean isSuccess() {
        return success;
    }

    public AgentDetailResult getData() {
        return data;
    }

    public ApiError getError() {
        return error;
    }
}
