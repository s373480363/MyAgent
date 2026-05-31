package com.myagent.workflow.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 从版本复制生成新草稿请求。
 */
public final class CopyWorkflowDraftFromVersionRequest {

    /**
     * 来源工作流版本主键。
     */
    @NotNull(message = "sourceWorkflowVersionId 不能为空。")
    @Min(value = 1, message = "sourceWorkflowVersionId 必须大于 0。")
    private Long sourceWorkflowVersionId;

    public Long getSourceWorkflowVersionId() {
        return sourceWorkflowVersionId;
    }

    public void setSourceWorkflowVersionId(Long sourceWorkflowVersionId) {
        this.sourceWorkflowVersionId = sourceWorkflowVersionId;
    }
}
