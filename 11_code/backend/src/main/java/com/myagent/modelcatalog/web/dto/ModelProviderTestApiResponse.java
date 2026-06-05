package com.myagent.modelcatalog.web.dto;

import com.myagent.common.api.ApiError;
import com.myagent.modelcatalog.application.result.ModelProviderTestResult;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 模型供应商连接测试统一响应。
 */
@Schema(name = "ModelProviderTestApiResponse", description = "模型供应商连接测试统一响应。")
public record ModelProviderTestApiResponse(
        boolean success,
        ModelProviderTestResult data,
        ApiError error
) {
}
