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
 * 运行接口 OpenAPI 契约测试。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class RunOpenApiContractTests {

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
     * 运行接口必须导出正式 DTO 响应契约。
     *
     * @throws Exception 请求或解析失败时抛出
     */
    @Test
    void runApisExportConcreteResponseContracts() throws Exception {
        JsonNode apiDocs = readApiDocs();

        assertThat(apiDocs.at("/paths/~1api~1agents~1{agentKey}~1runs/post/responses/200/content/*~1*/schema/$ref").asText())
                .isEqualTo("#/components/schemas/RunApiResponse");
        assertThat(apiDocs.at("/paths/~1api~1agents~1{agentId}~1debug-runs/post/responses/200/content/*~1*/schema/$ref").asText())
                .isEqualTo("#/components/schemas/RunApiResponse");
        assertThat(apiDocs.at("/paths/~1api~1runs/get/responses/200/content/*~1*/schema/$ref").asText())
                .isEqualTo("#/components/schemas/RunPageApiResponse");
        assertThat(apiDocs.at("/paths/~1api~1runs~1{runId}/get/responses/200/content/*~1*/schema/$ref").asText())
                .isEqualTo("#/components/schemas/RunDetailApiResponse");
        assertThat(apiDocs.at("/components/schemas/RunDetailResult/properties/nodeRuns/type").asText())
                .isEqualTo("array");
        assertThat(apiDocs.at("/components/schemas/RunDetailResult/properties/traceEvents/type").asText())
                .isEqualTo("array");
        assertThat(apiDocs.at("/components/schemas/TraceEventResult/properties/detailJson").isMissingNode())
                .isTrue();
        assertThat(apiDocs.at("/components/schemas/TraceEventResult/properties/detail/$ref").asText())
                .isEqualTo("#/components/schemas/JsonNode");
        assertThat(apiDocs.at("/components/schemas/TraceEventResult/properties/evalRunId/type").asText())
                .isEqualTo("string");
    }

    /**
     * 读取 OpenAPI 文档。
     *
     * @return OpenAPI 文档树
     * @throws Exception 请求或解析失败时抛出
     */
    private JsonNode readApiDocs() throws Exception {
        String content = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(content);
    }
}
