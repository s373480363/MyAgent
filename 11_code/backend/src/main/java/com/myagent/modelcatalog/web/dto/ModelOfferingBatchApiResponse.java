package com.myagent.modelcatalog.web.dto;

import com.myagent.common.api.ApiError;
import com.myagent.modelcatalog.application.result.ModelOfferingBatchResult;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 模型供应项批量查询统一响应。
 */
@Schema(name = "ModelOfferingBatchApiResponse", description = "模型供应项批量查询统一响应。")
public record ModelOfferingBatchApiResponse(
        boolean success,
        ModelOfferingBatchResult data,
        ApiError error
) {
}
