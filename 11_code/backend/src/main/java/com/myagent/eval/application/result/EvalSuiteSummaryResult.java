package com.myagent.eval.application.result;

/**
 * 验收套件摘要。
 *
 * @param suiteId 套件主键
 * @param name 套件名称
 */
public record EvalSuiteSummaryResult(long suiteId, String name) {
}
