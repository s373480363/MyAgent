package com.myagent.modelcatalog.web.dto;

import com.myagent.common.api.ApiError;
import com.myagent.common.api.PageResponse;
import com.myagent.modelcatalog.application.result.ModelOfferingDescriptor;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 模型供应项分页统一响应。
 */
@Schema(name = "ModelOfferingPageApiResponse", description = "模型供应项分页统一响应。")
public record ModelOfferingPageApiResponse(
        boolean success,
        PageResponse<ModelOfferingDescriptor> data,
        ApiError error
) {
}
