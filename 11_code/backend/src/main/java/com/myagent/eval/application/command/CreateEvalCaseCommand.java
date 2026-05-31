package com.myagent.eval.application.command;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 创建验收用例命令。
 *
 * @param suiteId 验收套件主键
 * @param caseNo 用例编号
 * @param title 用例标题
 * @param input 输入 JSON
 * @param referenceAnswer 参考答案
 * @param assertions 断言规则
 * @param scoreRule 评分规则
 * @param critical 是否关键用例
 * @param description 用例说明
 */
public record CreateEvalCaseCommand(
        long suiteId,
        String caseNo,
        String title,
        JsonNode input,
        JsonNode referenceAnswer,
        JsonNode assertions,
        JsonNode scoreRule,
        boolean critical,
        String description
) {
}
