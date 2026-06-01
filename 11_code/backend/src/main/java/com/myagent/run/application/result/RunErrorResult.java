package com.myagent.run.application.result;

import com.myagent.common.api.ApiError;

import java.util.List;

/**
 * 运行错误结果。
 *
 * @param code 错误码
 * @param message 错误消息
 * @param details 字段级错误明细
 */
public record RunErrorResult(String code, String message, List<ApiError.Detail> details) {
}
