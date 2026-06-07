package com.myagent.eval.application;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * judge LLM 执行结果。
 *
 * @param judgeResult 结构化判定结果
 * @param judgeRawText 原始输出
 * @param judgeModelOfferingKey 实际使用的模型供应项
 * @param judgePromptVersion 提示词版本
 * @param errorMessage 顶层失败摘要
 */
record EvalJudgeEvaluation(
        JsonNode judgeResult,
        String judgeRawText,
        String judgeModelOfferingKey,
        String judgePromptVersion,
        String errorMessage
) {
}
