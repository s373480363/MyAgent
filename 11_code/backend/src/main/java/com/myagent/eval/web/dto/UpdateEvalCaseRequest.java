package com.myagent.eval.web.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;

/**
 * 更新验收用例请求。
 *
 * @param title 用例标题
 * @param input 用例输入 JSON
 * @param referenceSample 参考样例 JSON
 * @param judgeRule 自然语言验收规则
 * @param hardChecks 硬约束配置数组
 * @param critical 是否为关键用例
 * @param description 用例说明
 */
public record UpdateEvalCaseRequest(
        @NotBlank String title,
        JsonNode input,
        JsonNode referenceSample,
        String judgeRule,
        JsonNode hardChecks,
        boolean critical,
        String description
) {
}
