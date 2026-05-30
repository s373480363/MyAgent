package com.myagent.schema.web.dto;

import com.myagent.common.api.ApiError;
import com.myagent.schema.application.result.SchemaDetailResult;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Schema 详情统一响应的 OpenAPI 契约对象。
 *
 * <p>运行时代码仍返回 ApiResponse&lt;SchemaDetailResult&gt;，该对象只用于让 OpenAPI 保留 data 的具体类型。</p>
 */
@Schema(name = "SchemaDetailApiResponse", description = "Schema 详情统一响应。")
public final class SchemaDetailApiResponse {

    @Schema(description = "业务是否成功。", requiredMode = Schema.RequiredMode.REQUIRED)
    private final boolean success;

    @Schema(description = "Schema 详情。")
    private final SchemaDetailResult data;

    @Schema(description = "错误对象。")
    private final ApiError error;

    /**
     * 构造 Schema 详情统一响应契约。
     *
     * @param success 业务是否成功
     * @param data Schema 详情
     * @param error 错误对象
     */
    public SchemaDetailApiResponse(boolean success, SchemaDetailResult data, ApiError error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    public boolean isSuccess() {
        return success;
    }

    public SchemaDetailResult getData() {
        return data;
    }

    public ApiError getError() {
        return error;
    }
}
