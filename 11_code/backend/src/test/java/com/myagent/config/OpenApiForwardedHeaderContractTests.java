package com.myagent.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * OpenAPI 正式入口地址契约测试。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class OpenApiForwardedHeaderContractTests {

    /**
     * MockMvc。
     */
    @Autowired
    private MockMvc mockMvc;

    /**
     * JSON 对象映射器。
     */
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * OpenAPI 输出必须基于正式入口请求头生成唯一服务器地址。
     *
     * @throws Exception 请求或解析失败时抛出
     */
    @Test
    void apiDocsUseForwardedHeadersAsFormalServerUrl() throws Exception {
        String content = mockMvc.perform(get("/v3/api-docs")
                        .header("Host", "127.0.0.1:18080")
                        .header("X-Forwarded-Host", "127.0.0.1:18080")
                        .header("X-Forwarded-Port", "18080")
                        .header("X-Forwarded-Proto", "http"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        JsonNode apiDocs = objectMapper.readTree(content);
        assertThat(apiDocs.at("/servers/0/url").asText()).isEqualTo("http://127.0.0.1:18080");
    }
}
