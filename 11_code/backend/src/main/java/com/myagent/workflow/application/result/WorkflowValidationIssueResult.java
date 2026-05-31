package com.myagent.workflow.application.result;

import com.myagent.common.api.ApiError;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 工作流校验问题。
 */
@Schema(name = "WorkflowValidationIssueResult", description = "工作流校验问题。")
public final class WorkflowValidationIssueResult {

    /**
     * 稳定错误码。
     */
    @Schema(description = "稳定错误码。", example = "WORKFLOW_VALIDATION_FAILED", requiredMode = Schema.RequiredMode.REQUIRED)
    private final String code;

    /**
     * 中文错误消息。
     */
    @Schema(description = "中文错误消息。", requiredMode = Schema.RequiredMode.REQUIRED)
    private final String message;

    /**
     * 字段级错误明细。
     */
    @Schema(description = "字段级错误明细。")
    private final List<ApiError.Detail> details;

    /**
     * 构造校验问题。
     *
     * @param code 稳定错误码
     * @param message 中文错误消息
     * @param details 字段级错误明细
     */
    public WorkflowValidationIssueResult(String code, String message, List<ApiError.Detail> details) {
        this.code = code;
        this.message = message;
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public List<ApiError.Detail> getDetails() {
        return details;
    }
}
