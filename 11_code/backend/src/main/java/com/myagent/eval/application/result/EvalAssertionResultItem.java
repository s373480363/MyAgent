package com.myagent.eval.application.result;

/**
 * 单条验收断言结果。
 *
 * @param type 断言类型
 * @param passed 是否通过
 * @param message 断言消息
 */
public record EvalAssertionResultItem(String type, boolean passed, String message) {
}
