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
 * 节点验收接口 OpenAPI 契约测试。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class EvalOpenApiContractTests {

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
     * Eval 接口必须导出正式冻结响应 DTO。
     *
     * @throws Exception 请求或解析失败时抛出
     */
    @Test
    void evalApisExportConcreteResponseContracts() throws Exception {
        JsonNode apiDocs = readApiDocs();

        assertThat(apiDocs.at("/paths/~1api~1eval-suites/get/responses/200/content/*~1*/schema/$ref").asText())
                .isEqualTo("#/components/schemas/EvalSuitePageApiResponse");
        assertThat(apiDocs.at("/paths/~1api~1eval-suites/post/responses/200/content/*~1*/schema/$ref").asText())
                .isEqualTo("#/components/schemas/EvalSuiteApiResponse");
        assertThat(apiDocs.at("/paths/~1api~1eval-suites~1{suiteId}~1runs/post/responses/200/content/*~1*/schema/$ref").asText())
                .isEqualTo("#/components/schemas/EvalRunApiResponse");
        assertThat(apiDocs.at("/paths/~1api~1eval-runs~1{evalRunId}/get/responses/200/content/*~1*/schema/$ref").asText())
                .isEqualTo("#/components/schemas/EvalRunDetailApiResponse");
        assertThat(apiDocs.at("/paths/~1api~1eval-runs~1{evalRunId}~1results/get/responses/200/content/*~1*/schema/$ref").asText())
                .isEqualTo("#/components/schemas/EvalRunResultPageApiResponse");
        assertThat(apiDocs.at("/paths/~1api~1eval-suites~1{suiteId}~1run-history/get/responses/200/content/*~1*/schema/$ref").asText())
                .isEqualTo("#/components/schemas/EvalRunHistoryPageApiResponse");
        assertThat(apiDocs.at("/components/schemas/EvalRunDetailResult/properties/failureSummary/type").asText())
                .isEqualTo("array");
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
