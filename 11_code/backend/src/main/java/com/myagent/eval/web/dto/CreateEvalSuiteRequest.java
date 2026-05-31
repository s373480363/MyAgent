package com.myagent.eval.web.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

/**
 * 创建验收套件请求。
 *
 * @param agentId Agent 主键
 * @param workflowVersionId 工作流版本主键
 * @param nodeId 节点标识
 * @param name 套件名称
 * @param goal 验收目标
 * @param passThreshold 通过率阈值
 */
public record CreateEvalSuiteRequest(
        @Min(1) long agentId,
        @Min(1) long workflowVersionId,
        @NotBlank String nodeId,
        @NotBlank String name,
        String goal,
        @DecimalMin("0") @DecimalMax("100") BigDecimal passThreshold
) {
}
