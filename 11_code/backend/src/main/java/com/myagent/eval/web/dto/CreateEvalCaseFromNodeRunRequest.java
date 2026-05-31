package com.myagent.eval.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * 从 NodeRun 创建验收用例请求。
 *
 * @param suiteId 套件主键
 * @param title 用例标题
 * @param description 用例说明
 */
public record CreateEvalCaseFromNodeRunRequest(
        @Min(1) long suiteId,
        @NotBlank String title,
        String description
) {
}
