package com.myagent.modelcatalog.web.dto;

import com.myagent.modelcatalog.domain.ModelProviderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 创建模型供应商请求。
 */
public final class CreateModelProviderRequest {

    /**
     * 供应商标识。
     */
    @NotBlank(message = "providerKey 不能为空。")
    private String providerKey;

    /**
     * 名称。
     */
    @NotBlank(message = "name 不能为空。")
    private String name;

    /**
     * 类型。
     */
    @NotNull(message = "providerType 不能为空。")
    private ModelProviderType providerType;

    /**
     * Base URL。
     */
    @NotBlank(message = "baseUrl 不能为空。")
    private String baseUrl;

    /**
     * API Key。
     */
    private String apiKey;

    /**
     * 描述。
     */
    private String description;

    public String getProviderKey() {
        return providerKey;
    }

    public void setProviderKey(String providerKey) {
        this.providerKey = providerKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ModelProviderType getProviderType() {
        return providerType;
    }

    public void setProviderType(ModelProviderType providerType) {
        this.providerType = providerType;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
