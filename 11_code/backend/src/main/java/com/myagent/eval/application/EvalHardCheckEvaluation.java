package com.myagent.eval.application;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * hardChecks 执行结果。
 *
 * @param passed 是否全部通过
 * @param hardCheckResults 逐条结果数组
 * @param errorMessage 顶层失败摘要
 */
record EvalHardCheckEvaluation(boolean passed, JsonNode hardCheckResults, String errorMessage) {
}
