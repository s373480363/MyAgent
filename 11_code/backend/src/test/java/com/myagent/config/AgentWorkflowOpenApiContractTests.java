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
 * Agent 与工作流接口 OpenAPI 契约测试。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class AgentWorkflowOpenApiContractTests {

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
     * Agent 与工作流接口必须导出具体响应契约。
     *
     * @throws Exception 请求或解析失败时抛出
     */
    @Test
    void agentAndWorkflowApisExportConcreteResponseContracts() throws Exception {
        JsonNode apiDocs = readApiDocs();

        assertThat(apiDocs.at("/paths/~1api~1agents/get/responses/200/content/*~1*/schema/$ref").asText())
                .isEqualTo("#/components/schemas/AgentPageApiResponse");
        assertThat(apiDocs.at("/paths/~1api~1agents/post/responses/200/content/*~1*/schema/$ref").asText())
                .isEqualTo("#/components/schemas/AgentDetailApiResponse");
        assertThat(apiDocs.at("/paths/~1api~1agents~1{agentId}/get/responses/200/content/*~1*/schema/$ref").asText())
                .isEqualTo("#/components/schemas/AgentDetailApiResponse");
        assertThat(apiDocs.at("/paths/~1api~1agents~1{agentId}~1workflow-draft/get/responses/200/content/*~1*/schema/$ref").asText())
                .isEqualTo("#/components/schemas/WorkflowDraftApiResponse");
        assertThat(apiDocs.at("/paths/~1api~1agents~1{agentId}~1workflow-draft~1publish/post/responses/200/content/*~1*/schema/$ref").asText())
                .isEqualTo("#/components/schemas/WorkflowPublishApiResponse");
        assertThat(apiDocs.at("/paths/~1api~1agents~1{agentId}~1workflow-versions/get/responses/200/content/*~1*/schema/$ref").asText())
                .isEqualTo("#/components/schemas/WorkflowVersionPageApiResponse");
        assertThat(apiDocs.at("/paths/~1api~1agents~1{agentId}~1workflow-versions~1{workflowVersionId}/get/responses/200/content/*~1*/schema/$ref").asText())
                .isEqualTo("#/components/schemas/WorkflowVersionDetailApiResponse");
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
