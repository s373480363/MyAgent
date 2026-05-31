package com.myagent.eval.web.dto;

import com.myagent.common.api.ApiError;
import com.myagent.common.api.PageResponse;
import com.myagent.eval.application.result.EvalRunResultItemResult;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 验收运行结果明细统一响应。
 *
 * @param success 成功标记
 * @param data 分页数据
 * @param error 错误对象
 */
@Schema(name = "EvalRunResultPageApiResponse", description = "验收运行结果明细统一响应。")
public record EvalRunResultPageApiResponse(boolean success, PageResponse<EvalRunResultItemResult> data, ApiError error) {
}
