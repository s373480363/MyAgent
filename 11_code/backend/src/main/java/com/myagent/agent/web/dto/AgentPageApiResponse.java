package com.myagent.agent.web.dto;

import com.myagent.agent.application.result.AgentListItemResult;
import com.myagent.common.api.ApiError;
import com.myagent.common.api.PageResponse;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Agent 分页统一响应。
 */
@Schema(name = "AgentPageApiResponse", description = "Agent 分页统一响应。")
public final class AgentPageApiResponse {

    @Schema(description = "业务是否成功。", requiredMode = Schema.RequiredMode.REQUIRED)
    private final boolean success;

    @Schema(description = "Agent 分页数据。")
    private final PageResponse<AgentListItemResult> data;

    @Schema(description = "错误对象。")
    private final ApiError error;

    /**
     * 构造统一响应。
     *
     * @param success 成功标记
     * @param data 分页数据
     * @param error 错误对象
     */
    public AgentPageApiResponse(boolean success, PageResponse<AgentListItemResult> data, ApiError error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    public boolean isSuccess() {
        return success;
    }

    public PageResponse<AgentListItemResult> getData() {
        return data;
    }

    public ApiError getError() {
        return error;
    }
}
