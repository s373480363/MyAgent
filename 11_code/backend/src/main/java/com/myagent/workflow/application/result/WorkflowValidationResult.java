package com.myagent.workflow.application.result;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 工作流校验结果。
 */
@Schema(name = "WorkflowValidationResult", description = "工作流校验结果。")
public final class WorkflowValidationResult {

    /**
     * 是否通过。
     */
    @Schema(description = "是否通过。", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    private final boolean valid;

    /**
     * 校验问题列表。
     */
    @Schema(description = "校验问题列表。", requiredMode = Schema.RequiredMode.REQUIRED)
    private final List<WorkflowValidationIssueResult> errors;

    /**
     * 构造校验结果。
     *
     * @param valid 是否通过
     * @param errors 校验问题列表
     */
    public WorkflowValidationResult(boolean valid, List<WorkflowValidationIssueResult> errors) {
        this.valid = valid;
        this.errors = errors == null ? List.of() : List.copyOf(errors);
    }

    public boolean isValid() {
        return valid;
    }

    public List<WorkflowValidationIssueResult> getErrors() {
        return errors;
    }
}
