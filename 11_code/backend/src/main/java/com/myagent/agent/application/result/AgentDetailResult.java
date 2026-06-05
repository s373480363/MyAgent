package com.myagent.agent.application.result;

import com.myagent.common.domain.EnableStatus;
import com.myagent.workflow.application.result.WorkflowVersionSummaryResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Agent 详情结果。
 */
@Schema(name = "AgentDetailResult", description = "Agent 详情结果。")
public final class AgentDetailResult {

    /**
     * Agent 主键。
     */
    @Schema(description = "Agent 主键。", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private final long agentId;

    /**
     * Agent 业务标识。
     */
    @Schema(description = "Agent 业务标识。", example = "summary-agent", requiredMode = Schema.RequiredMode.REQUIRED)
    private final String agentKey;

    /**
     * 名称。
     */
    @Schema(description = "名称。", example = "摘要 Agent", requiredMode = Schema.RequiredMode.REQUIRED)
    private final String name;

    /**
     * 描述。
     */
    @Schema(description = "描述。")
    private final String description;

    /**
     * 状态。
     */
    @Schema(description = "状态。", requiredMode = Schema.RequiredMode.REQUIRED)
    private final EnableStatus status;

    /**
     * 系统提示词。
     */
    @Schema(description = "系统提示词。")
    private final String systemPrompt;

    /**
     * 默认模型供应项标识。
     */
    @Schema(description = "默认模型供应项标识。", example = "openai.gpt_4_1_mini")
    private final String defaultModelOfferingKey;

    /**
     * 温度。
     */
    @Schema(description = "温度。", example = "0.2")
    private final BigDecimal temperature;

    /**
     * Agent 默认总超时。
     */
    @Schema(description = "Agent 默认总超时（秒）。", example = "600", requiredMode = Schema.RequiredMode.REQUIRED)
    private final int timeoutSeconds;

    /**
     * Agent 默认最大步数。
     */
    @Schema(description = "Agent 默认最大步数。", example = "30", requiredMode = Schema.RequiredMode.REQUIRED)
    private final int maxSteps;

    /**
     * 当前草稿版本摘要。
     */
    @Schema(description = "当前草稿版本摘要。")
    private final WorkflowVersionSummaryResult currentDraftWorkflow;

    /**
     * 当前发布版本摘要。
     */
    @Schema(description = "当前发布版本摘要。")
    private final WorkflowVersionSummaryResult currentPublishedWorkflow;

    /**
     * 历史版本入口摘要。
     */
    @Schema(description = "历史版本入口摘要。", requiredMode = Schema.RequiredMode.REQUIRED)
    private final HistoryVersionSummaryResult historyVersionSummary;

    /**
     * 更新时间。
     */
    @Schema(description = "更新时间。", requiredMode = Schema.RequiredMode.REQUIRED)
    private final Instant updatedAt;

    /**
     * 构造 Agent 详情。
     *
     * @param agentId Agent 主键
     * @param agentKey Agent 业务标识
     * @param name 名称
     * @param description 描述
     * @param status 状态
     * @param systemPrompt 系统提示词
     * @param defaultModelOfferingKey 默认模型供应项标识
     * @param temperature 温度
     * @param timeoutSeconds Agent 默认总超时
     * @param maxSteps Agent 默认最大步数
     * @param currentDraftWorkflow 当前草稿版本摘要
     * @param currentPublishedWorkflow 当前发布版本摘要
     * @param historyVersionSummary 历史版本入口摘要
     * @param updatedAt 更新时间
     */
    public AgentDetailResult(
            long agentId,
            String agentKey,
            String name,
            String description,
            EnableStatus status,
            String systemPrompt,
            String defaultModelOfferingKey,
            BigDecimal temperature,
            int timeoutSeconds,
            int maxSteps,
            WorkflowVersionSummaryResult currentDraftWorkflow,
            WorkflowVersionSummaryResult currentPublishedWorkflow,
            HistoryVersionSummaryResult historyVersionSummary,
            Instant updatedAt
    ) {
        this.agentId = agentId;
        this.agentKey = agentKey;
        this.name = name;
        this.description = description;
        this.status = status;
        this.systemPrompt = systemPrompt;
        this.defaultModelOfferingKey = defaultModelOfferingKey;
        this.temperature = temperature;
        this.timeoutSeconds = timeoutSeconds;
        this.maxSteps = maxSteps;
        this.currentDraftWorkflow = currentDraftWorkflow;
        this.currentPublishedWorkflow = currentPublishedWorkflow;
        this.historyVersionSummary = historyVersionSummary;
        this.updatedAt = updatedAt;
    }

    public long getAgentId() {
        return agentId;
    }

    public String getAgentKey() {
        return agentKey;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public EnableStatus getStatus() {
        return status;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public String getDefaultModelOfferingKey() {
        return defaultModelOfferingKey;
    }

    public BigDecimal getTemperature() {
        return temperature;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public int getMaxSteps() {
        return maxSteps;
    }

    public WorkflowVersionSummaryResult getCurrentDraftWorkflow() {
        return currentDraftWorkflow;
    }

    public WorkflowVersionSummaryResult getCurrentPublishedWorkflow() {
        return currentPublishedWorkflow;
    }

    public HistoryVersionSummaryResult getHistoryVersionSummary() {
        return historyVersionSummary;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
