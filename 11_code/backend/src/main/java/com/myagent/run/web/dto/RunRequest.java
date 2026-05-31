package com.myagent.run.web.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 正式运行请求。
 */
@Schema(name = "RunRequest", description = "正式运行请求。")
public final class RunRequest {

    /**
     * 运行输入。
     */
    @Schema(description = "运行输入。")
    private JsonNode input;

    /**
     * 返回运行输入。
     *
     * @return 运行输入
     */
    public JsonNode getInput() {
        return input;
    }

    /**
     * 设置运行输入。
     *
     * @param input 运行输入
     */
    public void setInput(JsonNode input) {
        this.input = input;
    }
}
