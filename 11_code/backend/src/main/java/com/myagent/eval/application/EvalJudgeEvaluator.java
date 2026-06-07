package com.myagent.eval.application;

/**
 * judge LLM 执行器。
 */
public interface EvalJudgeEvaluator {

    /**
     * 执行 judge 判定。
     *
     * @param request judge 请求
     * @return judge 结果
     */
    EvalJudgeEvaluation evaluate(EvalJudgeRequest request);
}
