package com.myagent.modelcatalog.web.dto;

import com.myagent.common.api.ApiError;
import com.myagent.modelcatalog.application.result.ModelProviderResult;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 模型供应商详情统一响应。
 */
@Schema(name = "ModelProviderDetailApiResponse", description = "模型供应商详情统一响应。")
public record ModelProviderDetailApiResponse(
        boolean success,
        ModelProviderResult data,
        ApiError error
) {
}
