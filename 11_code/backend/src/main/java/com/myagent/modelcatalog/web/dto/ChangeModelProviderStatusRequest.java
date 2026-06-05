package com.myagent.modelcatalog.web.dto;

import com.myagent.common.domain.EnableStatus;
import jakarta.validation.constraints.NotNull;

/**
 * 修改模型供应商状态请求。
 */
public final class ChangeModelProviderStatusRequest {

    /**
     * 目标状态。
     */
    @NotNull(message = "status 不能为空。")
    private EnableStatus status;

    public EnableStatus getStatus() {
        return status;
    }

    public void setStatus(EnableStatus status) {
        this.status = status;
    }
}
