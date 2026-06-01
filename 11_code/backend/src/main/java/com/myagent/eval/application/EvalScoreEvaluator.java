package com.myagent.eval.application;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Eval 可选评分执行器。
 */
public interface EvalScoreEvaluator {

    /**
     * 执行可选 LLM 评分。
     *
     * @param request 评分请求
     * @return 评分结果；未配置评分规则时返回 null
     */
    JsonNode evaluate(EvalScoreRequest request);
}
