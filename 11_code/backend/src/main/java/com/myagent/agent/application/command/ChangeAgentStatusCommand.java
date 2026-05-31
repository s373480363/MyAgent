package com.myagent.agent.application.command;

import com.myagent.common.domain.EnableStatus;

/**
 * 修改 Agent 状态命令。
 *
 * @param agentId Agent 主键
 * @param status 目标状态
 */
public record ChangeAgentStatusCommand(long agentId, EnableStatus status) {
}
