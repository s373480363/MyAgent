package com.myagent.modelcatalog.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 更新模型供应商请求。
 */
public final class UpdateModelProviderRequest {

    /**
     * 名称。
     */
    @NotBlank(message = "name 不能为空。")
    private String name;

    /**
     * Base URL。
     */
    @NotBlank(message = "baseUrl 不能为空。")
    private String baseUrl;

    /**
     * 描述。
     */
    private String description;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
