package com.myagent.externalagent.web.dto;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 测试外部 Agent 请求。
 */
public final class TestExternalAgentRequest {

    /**
     * 测试提示词。
     */
    private String prompt;

    /**
     * 输入 JSON。
     */
    private JsonNode input;

    /**
     * 返回测试提示词。
     *
     * @return 测试提示词
     */
    public String getPrompt() {
        return prompt;
    }

    /**
     * 设置测试提示词。
     *
     * @param prompt 测试提示词
     */
    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    /**
     * 返回输入 JSON。
     *
     * @return 输入 JSON
     */
    public JsonNode getInput() {
        return input;
    }

    /**
     * 设置输入 JSON。
     *
     * @param input 输入 JSON
     */
    public void setInput(JsonNode input) {
        this.input = input;
    }
}
