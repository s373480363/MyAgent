package com.myagent.agent.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

/**
 * 更新 Agent 请求。
 */
public final class UpdateAgentRequest {

    /**
     * 名称。
     */
    @NotBlank(message = "name 不能为空。")
    private String name;

    /**
     * 描述。
     */
    private String description;

    /**
     * 系统提示词。
     */
    private String systemPrompt;

    /**
     * 默认模型。
     */
    private String defaultModel;

    /**
     * 温度。
     */
    private BigDecimal temperature;

    /**
     * Agent 默认总超时。
     */
    @Min(value = 1, message = "timeoutSeconds 必须大于 0。")
    private Integer timeoutSeconds;

    /**
     * Agent 默认最大步数。
     */
    @Min(value = 1, message = "maxSteps 必须大于 0。")
    private Integer maxSteps;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public String getDefaultModel() {
        return defaultModel;
    }

    public void setDefaultModel(String defaultModel) {
        this.defaultModel = defaultModel;
    }

    public BigDecimal getTemperature() {
        return temperature;
    }

    public void setTemperature(BigDecimal temperature) {
        this.temperature = temperature;
    }

    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public Integer getMaxSteps() {
        return maxSteps;
    }

    public void setMaxSteps(Integer maxSteps) {
        this.maxSteps = maxSteps;
    }
}
