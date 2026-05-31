package com.myagent.externalagent.web.dto;

import jakarta.validation.Valid;

import java.util.List;

/**
 * 更新外部 Agent 敏感 secret 请求。
 */
public final class UpdateExternalAgentSecretsRequest {

    /**
     * 覆盖写入项。
     */
    @Valid
    private List<ExternalAgentSecretHeaderRequest> items;

    /**
     * 显式清空 header 名称列表。
     */
    private List<String> clearHeaderNames;

    public List<ExternalAgentSecretHeaderRequest> getItems() {
        return items;
    }

    public void setItems(List<ExternalAgentSecretHeaderRequest> items) {
        this.items = items;
    }

    public List<String> getClearHeaderNames() {
        return clearHeaderNames;
    }

    public void setClearHeaderNames(List<String> clearHeaderNames) {
        this.clearHeaderNames = clearHeaderNames;
    }
}
