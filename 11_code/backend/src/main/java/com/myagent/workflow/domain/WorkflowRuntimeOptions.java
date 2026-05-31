package com.myagent.workflow.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 工作流版本运行约束快照。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "WorkflowRuntimeOptions", description = "工作流版本运行约束快照。")
public final class WorkflowRuntimeOptions {

    /**
     * 工作流总超时。
     */
    @Schema(description = "工作流总超时（秒）。", example = "600", requiredMode = Schema.RequiredMode.REQUIRED)
    private final int timeoutSeconds;

    /**
     * 最大执行步数。
     */
    @Schema(description = "最大执行步数。", example = "30", requiredMode = Schema.RequiredMode.REQUIRED)
    private final int maxSteps;

    /**
     * 最大 Agent 调用深度。
     */
    @Schema(description = "最大 Agent 调用深度。", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
    private final int maxAgentCallDepth;

    /**
     * 构造运行约束。
     *
     * @param timeoutSeconds 工作流总超时
     * @param maxSteps 最大执行步数
     * @param maxAgentCallDepth 最大 Agent 调用深度
     */
    public WorkflowRuntimeOptions(int timeoutSeconds, int maxSteps, int maxAgentCallDepth) {
        this.timeoutSeconds = timeoutSeconds;
        this.maxSteps = maxSteps;
        this.maxAgentCallDepth = maxAgentCallDepth;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public int getMaxSteps() {
        return maxSteps;
    }

    public int getMaxAgentCallDepth() {
        return maxAgentCallDepth;
    }
}
