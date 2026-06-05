package com.myagent.modelcatalog.application.command;

import com.myagent.common.domain.EnableStatus;

/**
 * 修改模型供应商状态命令。
 *
 * @param providerId 供应商主键
 * @param status 状态
 */
public record ChangeModelProviderStatusCommand(
        long providerId,
        EnableStatus status
) {
}
