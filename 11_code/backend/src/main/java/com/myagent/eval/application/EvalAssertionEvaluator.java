package com.myagent.eval.application;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 验收断言执行器。
 */
interface EvalAssertionEvaluator {

    /**
     * 执行确定性断言。
     *
     * @param output 节点输出
     * @param assertions 断言定义
     * @param schemaValidationResultJson 节点 Schema 校验结果
     * @return 断言结果
     */
    EvalAssertionEvaluation evaluate(JsonNode output, JsonNode assertions, JsonNode schemaValidationResultJson);
}
