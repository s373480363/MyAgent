package com.myagent.run.application.result;

/**
 * 运行错误结果。
 *
 * @param code 错误码
 * @param message 错误消息
 */
public record RunErrorResult(String code, String message) {
}
