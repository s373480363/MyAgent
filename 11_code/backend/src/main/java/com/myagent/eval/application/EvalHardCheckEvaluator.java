package com.myagent.eval.application;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * hardChecks 执行器。
 */
public interface EvalHardCheckEvaluator {

    /**
     * 执行 hardChecks。
     *
     * @param output 节点输出
     * @param hardChecks hardChecks 配置
     * @param schemaValidationResultJson 节点 Schema 校验结果
     * @return 执行结果
     */
    EvalHardCheckEvaluation evaluate(JsonNode output, JsonNode hardChecks, JsonNode schemaValidationResultJson);
}
