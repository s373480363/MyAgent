package com.myagent.eval.application.command;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 创建验收用例命令。
 *
 * @param suiteId 验收套件主键
 * @param caseNo 用例编号
 * @param title 用例标题
 * @param input 用例输入 JSON
 * @param referenceSample 参考样例 JSON
 * @param judgeRule 自然语言验收规则
 * @param hardChecks 硬约束配置数组
 * @param critical 是否为关键用例
 * @param description 用例说明
 */
public record CreateEvalCaseCommand(
        long suiteId,
        String caseNo,
        String title,
        JsonNode input,
        JsonNode referenceSample,
        String judgeRule,
        JsonNode hardChecks,
        boolean critical,
        String description
) {
}
