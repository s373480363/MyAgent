package com.myagent.eval.application;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 验收断言执行结果。
 *
 * @param passed 是否全部通过
 * @param assertionResults 断言明细数组
 * @param errorMessage 失败摘要
 */
record EvalAssertionEvaluation(boolean passed, JsonNode assertionResults, String errorMessage) {
}
