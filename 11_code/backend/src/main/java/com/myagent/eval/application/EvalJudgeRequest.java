package com.myagent.eval.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.myagent.workflow.domain.WorkflowNodeDefinition;

import java.math.BigDecimal;

/**
 * judge LLM 执行请求。
 *
 * @param judgeModelOfferingKey judge 模型供应项
 * @param judgeTemperature judge 温度
 * @param node 被验收节点
 * @param input 用例输入
 * @param referenceSample 参考样例
 * @param judgeRule 自然语言验收规则
 * @param output 实际输出
 * @param hardCheckResults hardChecks 结果
 */
public record EvalJudgeRequest(
        String judgeModelOfferingKey,
        BigDecimal judgeTemperature,
        WorkflowNodeDefinition node,
        JsonNode input,
        JsonNode referenceSample,
        String judgeRule,
        JsonNode output,
        JsonNode hardCheckResults
) {
}
