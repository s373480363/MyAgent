package com.myagent.modelcatalog.web.dto;

/**
 * 更新模型供应商密钥请求。
 */
public final class UpdateModelProviderSecretsRequest {

    /**
     * 新密钥。
     */
    private String apiKey;

    /**
     * 是否清空密钥。
     */
    private Boolean clearApiKey;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public Boolean getClearApiKey() {
        return clearApiKey;
    }

    public void setClearApiKey(Boolean clearApiKey) {
        this.clearApiKey = clearApiKey;
    }
}
