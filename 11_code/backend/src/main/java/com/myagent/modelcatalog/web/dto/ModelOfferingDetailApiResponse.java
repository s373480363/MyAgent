package com.myagent.modelcatalog.web.dto;

import com.myagent.common.api.ApiError;
import com.myagent.modelcatalog.application.result.ModelOfferingDescriptor;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 模型供应项详情统一响应。
 */
@Schema(name = "ModelOfferingDetailApiResponse", description = "模型供应项详情统一响应。")
public record ModelOfferingDetailApiResponse(
        boolean success,
        ModelOfferingDescriptor data,
        ApiError error
) {
}
