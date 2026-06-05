package com.myagent.modelcatalog.application.command;

import com.myagent.common.domain.EnableStatus;

/**
 * 修改模型供应项状态命令。
 *
 * @param offeringId 供应项主键
 * @param status 状态
 */
public record ChangeModelOfferingStatusCommand(
        long offeringId,
        EnableStatus status
) {
}
