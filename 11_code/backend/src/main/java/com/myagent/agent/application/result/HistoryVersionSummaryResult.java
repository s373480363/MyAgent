package com.myagent.agent.application.result;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * 历史版本入口摘要。
 */
@Schema(name = "HistoryVersionSummaryResult", description = "历史版本入口摘要。")
public final class HistoryVersionSummaryResult {

    /**
     * 历史版本总数。
     */
    @Schema(description = "历史版本总数。", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    private final long total;

    /**
     * 最近历史版本主键。
     */
    @Schema(description = "最近历史版本主键。", example = "9")
    private final Long latestWorkflowVersionId;

    /**
     * 最近历史版本号。
     */
    @Schema(description = "最近历史版本号。", example = "1")
    private final Integer latestVersionNo;

    /**
     * 最近历史发布时间。
     */
    @Schema(description = "最近历史发布时间。")
    private final Instant latestPublishedAt;

    /**
     * 构造历史版本摘要。
     *
     * @param total 历史版本总数
     * @param latestWorkflowVersionId 最近历史版本主键
     * @param latestVersionNo 最近历史版本号
     * @param latestPublishedAt 最近历史发布时间
     */
    public HistoryVersionSummaryResult(
            long total,
            Long latestWorkflowVersionId,
            Integer latestVersionNo,
            Instant latestPublishedAt
    ) {
        this.total = total;
        this.latestWorkflowVersionId = latestWorkflowVersionId;
        this.latestVersionNo = latestVersionNo;
        this.latestPublishedAt = latestPublishedAt;
    }

    public long getTotal() {
        return total;
    }

    public Long getLatestWorkflowVersionId() {
        return latestWorkflowVersionId;
    }

    public Integer getLatestVersionNo() {
        return latestVersionNo;
    }

    public Instant getLatestPublishedAt() {
        return latestPublishedAt;
    }
}
