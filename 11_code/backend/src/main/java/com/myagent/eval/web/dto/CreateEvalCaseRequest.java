package com.myagent.eval.web.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;

/**
 * 创建验收用例请求。
 *
 * @param caseNo 用例编号
 * @param title 用例标题
 * @param input 输入 JSON
 * @param referenceAnswer 参考答案
 * @param assertions 断言规则
 * @param scoreRule 评分规则
 * @param critical 是否关键用例
 * @param description 用例说明
 */
public record CreateEvalCaseRequest(
        @NotBlank String caseNo,
        @NotBlank String title,
        JsonNode input,
        JsonNode referenceAnswer,
        JsonNode assertions,
        JsonNode scoreRule,
        boolean critical,
        String description
) {
}
