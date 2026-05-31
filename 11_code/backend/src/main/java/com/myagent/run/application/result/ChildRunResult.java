package com.myagent.run.application.result;

/**
 * 子运行摘要。
 *
 * @param runId 子运行编号
 * @param summary 消息摘要
 */
public record ChildRunResult(String runId, String summary) {
}
