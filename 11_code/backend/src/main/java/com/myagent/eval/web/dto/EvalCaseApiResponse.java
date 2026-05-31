package com.myagent.eval.web.dto;

import com.myagent.common.api.ApiError;
import com.myagent.eval.application.result.EvalCaseResult;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 验收用例详情统一响应。
 *
 * @param success 成功标记
 * @param data 用例详情
 * @param error 错误对象
 */
@Schema(name = "EvalCaseApiResponse", description = "验收用例详情统一响应。")
public record EvalCaseApiResponse(boolean success, EvalCaseResult data, ApiError error) {
}
