package com.myagent.modelcatalog.web.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

/**
 * 创建模型供应项请求。
 */
public final class CreateModelOfferingRequest {

    /**
     * 供应项标识。
     */
    @NotBlank(message = "offeringKey 不能为空。")
    private String offeringKey;

    /**
     * 供应商标识。
     */
    @NotBlank(message = "providerKey 不能为空。")
    private String providerKey;

    /**
     * 模型身份键。
     */
    @NotBlank(message = "modelKey 不能为空。")
    private String modelKey;

    /**
     * 展示名称。
     */
    @NotBlank(message = "displayName 不能为空。")
    private String displayName;

    /**
     * 上游模型名。
     */
    @NotBlank(message = "upstreamModelName 不能为空。")
    private String upstreamModelName;

    /**
     * 默认温度。
     */
    @DecimalMin(value = "0", message = "defaultTemperature 不能小于 0。")
    @DecimalMax(value = "2", message = "defaultTemperature 不能大于 2。")
    private BigDecimal defaultTemperature;

    /**
     * 描述。
     */
    private String description;

    public String getOfferingKey() {
        return offeringKey;
    }

    public void setOfferingKey(String offeringKey) {
        this.offeringKey = offeringKey;
    }

    public String getProviderKey() {
        return providerKey;
    }

    public void setProviderKey(String providerKey) {
        this.providerKey = providerKey;
    }

    public String getModelKey() {
        return modelKey;
    }

    public void setModelKey(String modelKey) {
        this.modelKey = modelKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getUpstreamModelName() {
        return upstreamModelName;
    }

    public void setUpstreamModelName(String upstreamModelName) {
        this.upstreamModelName = upstreamModelName;
    }

    public BigDecimal getDefaultTemperature() {
        return defaultTemperature;
    }

    public void setDefaultTemperature(BigDecimal defaultTemperature) {
        this.defaultTemperature = defaultTemperature;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
