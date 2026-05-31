package com.myagent.eval.web.dto;

import com.myagent.common.api.ApiError;
import com.myagent.common.api.PageResponse;
import com.myagent.eval.application.result.EvalRunListItemResult;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 验收运行列表统一响应。
 *
 * @param success 成功标记
 * @param data 分页数据
 * @param error 错误对象
 */
@Schema(name = "EvalRunPageApiResponse", description = "验收运行列表统一响应。")
public record EvalRunPageApiResponse(boolean success, PageResponse<EvalRunListItemResult> data, ApiError error) {
}
