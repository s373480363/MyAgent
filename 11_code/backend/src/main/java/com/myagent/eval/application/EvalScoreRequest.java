package com.myagent.eval.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.myagent.agent.repository.AgentRecord;
import com.myagent.workflow.domain.WorkflowNodeDefinition;

/**
 * Eval LLM 评分请求。
 *
 * @param scoreRule 评分规则
 * @param agent Agent 主数据
 * @param node 被验收节点
 * @param input 用例输入
 * @param referenceAnswer 参考答案
 * @param output 实际输出
 * @param assertionResults 确定性断言结果
 * @param assertionPassed 确定性断言是否通过
 */
public record EvalScoreRequest(
        JsonNode scoreRule,
        AgentRecord agent,
        WorkflowNodeDefinition node,
        JsonNode input,
        JsonNode referenceAnswer,
        JsonNode output,
        JsonNode assertionResults,
        boolean assertionPassed
) {
}
