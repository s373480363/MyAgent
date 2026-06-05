package com.myagent.modelcatalog.web.dto;

import com.myagent.common.api.ApiError;
import com.myagent.common.api.PageResponse;
import com.myagent.modelcatalog.application.result.ModelProviderResult;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 模型供应商分页统一响应。
 */
@Schema(name = "ModelProviderPageApiResponse", description = "模型供应商分页统一响应。")
public record ModelProviderPageApiResponse(
        boolean success,
        PageResponse<ModelProviderResult> data,
        ApiError error
) {
}
