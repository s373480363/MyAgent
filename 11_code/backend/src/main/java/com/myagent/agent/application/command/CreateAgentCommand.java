package com.myagent.agent.application.command;

import java.math.BigDecimal;

/**
 * 创建 Agent 命令。
 *
 * @param agentKey Agent 业务标识
 * @param name 名称
 * @param description 描述
 * @param systemPrompt 系统提示词
 * @param defaultModelOfferingKey 默认模型供应项标识
 * @param temperature 温度
 * @param timeoutSeconds Agent 默认总超时
 * @param maxSteps Agent 默认最大步数
 */
public record CreateAgentCommand(
        String agentKey,
        String name,
        String description,
        String systemPrompt,
        String defaultModelOfferingKey,
        BigDecimal temperature,
        Integer timeoutSeconds,
        Integer maxSteps
) {
}
