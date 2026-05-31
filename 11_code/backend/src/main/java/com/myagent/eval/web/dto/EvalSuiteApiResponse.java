package com.myagent.eval.web.dto;

import com.myagent.common.api.ApiError;
import com.myagent.eval.application.result.EvalSuiteResult;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 验收套件详情统一响应。
 *
 * @param success 成功标记
 * @param data 套件详情
 * @param error 错误对象
 */
@Schema(name = "EvalSuiteApiResponse", description = "验收套件详情统一响应。")
public record EvalSuiteApiResponse(boolean success, EvalSuiteResult data, ApiError error) {
}
