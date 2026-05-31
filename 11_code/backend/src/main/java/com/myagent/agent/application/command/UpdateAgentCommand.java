package com.myagent.agent.application.command;

import java.math.BigDecimal;

/**
 * 更新 Agent 命令。
 *
 * @param agentId Agent 主键
 * @param name 名称
 * @param description 描述
 * @param systemPrompt 系统提示词
 * @param defaultModel 默认模型
 * @param temperature 温度
 * @param timeoutSeconds Agent 默认总超时
 * @param maxSteps Agent 默认最大步数
 */
public record UpdateAgentCommand(
        long agentId,
        String name,
        String description,
        String systemPrompt,
        String defaultModel,
        BigDecimal temperature,
        Integer timeoutSeconds,
        Integer maxSteps
) {
}
