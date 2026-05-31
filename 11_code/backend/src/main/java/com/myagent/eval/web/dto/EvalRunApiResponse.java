package com.myagent.eval.web.dto;

import com.myagent.common.api.ApiError;
import com.myagent.eval.application.result.EvalRunResult;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 验收运行创建统一响应。
 *
 * @param success 成功标记
 * @param data 验收运行结果
 * @param error 错误对象
 */
@Schema(name = "EvalRunApiResponse", description = "验收运行创建统一响应。")
public record EvalRunApiResponse(boolean success, EvalRunResult data, ApiError error) {
}
