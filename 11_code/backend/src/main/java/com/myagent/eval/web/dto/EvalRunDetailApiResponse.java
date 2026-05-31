package com.myagent.eval.web.dto;

import com.myagent.common.api.ApiError;
import com.myagent.eval.application.result.EvalRunDetailResult;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 验收运行详情统一响应。
 *
 * @param success 成功标记
 * @param data 运行详情
 * @param error 错误对象
 */
@Schema(name = "EvalRunDetailApiResponse", description = "验收运行详情统一响应。")
public record EvalRunDetailApiResponse(boolean success, EvalRunDetailResult data, ApiError error) {
}
