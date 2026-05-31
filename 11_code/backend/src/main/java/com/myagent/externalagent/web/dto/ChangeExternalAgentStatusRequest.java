package com.myagent.externalagent.web.dto;

import com.myagent.common.domain.EnableStatus;
import jakarta.validation.constraints.NotNull;

/**
 * 更新外部 Agent 状态请求。
 */
public final class ChangeExternalAgentStatusRequest {

    /**
     * 新状态。
     */
    @NotNull(message = "status 不能为空。")
    private EnableStatus status;

    /**
     * 返回新状态。
     *
     * @return 新状态
     */
    public EnableStatus getStatus() {
        return status;
    }

    /**
     * 设置新状态。
     *
     * @param status 新状态
     */
    public void setStatus(EnableStatus status) {
        this.status = status;
    }
}
