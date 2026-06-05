package com.myagent.modelcatalog.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 测试模型供应商请求。
 */
public final class TestModelProviderRequest {

    /**
     * 供应项标识。
     */
    @NotBlank(message = "offeringKey 不能为空。")
    private String offeringKey;

    /**
     * 测试提示词。
     */
    private String prompt;

    public String getOfferingKey() {
        return offeringKey;
    }

    public void setOfferingKey(String offeringKey) {
        this.offeringKey = offeringKey;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
}
