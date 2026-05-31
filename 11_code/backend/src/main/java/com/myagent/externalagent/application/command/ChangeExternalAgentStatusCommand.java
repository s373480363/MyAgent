package com.myagent.externalagent.application.command;

import com.myagent.common.domain.EnableStatus;

/**
 * 更新外部 Agent 状态命令。
 *
 * @param adapterId 外部 Agent 主键
 * @param status 新状态
 */
public record ChangeExternalAgentStatusCommand(long adapterId, EnableStatus status) {
}
