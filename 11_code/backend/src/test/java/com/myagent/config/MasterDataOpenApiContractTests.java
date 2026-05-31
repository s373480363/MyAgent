package com.myagent.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 主数据模块 OpenAPI 契约测试。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class MasterDataOpenApiContractTests {

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
     * 主数据接口必须导出具体响应契约，而不是退回泛型统一响应占位。
     *
     * @throws Exception 请求和解析失败时抛出
     */
    @Test
    void masterDataApisExportConcreteResponseContracts() throws Exception {
        JsonNode apiDocs = readApiDocs();

        assertThat(apiDocs.at("/paths/~1api~1settings/get/responses/200/content/*~1*/schema/$ref").asText())
                .isEqualTo("#/components/schemas/SettingsListApiResponse");
        assertThat(apiDocs.at("/paths/~1api~1settings/put/responses/200/content/*~1*/schema/$ref").asText())
                .isEqualTo("#/components/schemas/SettingsListApiResponse");
        assertThat(apiDocs.at("/paths/~1api~1java-methods/get/responses/200/content/*~1*/schema/$ref").asText())
                .isEqualTo("#/components/schemas/JavaMethodPageApiResponse");
        assertThat(apiDocs.at("/paths/~1api~1java-methods~1{methodId}/get/responses/200/content/*~1*/schema/$ref").asText())
                .isEqualTo("#/components/schemas/JavaMethodDetailApiResponse");
        assertThat(apiDocs.at("/paths/~1api~1tools/get/responses/200/content/*~1*/schema/$ref").asText())
                .isEqualTo("#/components/schemas/ToolPageApiResponse");
        assertThat(apiDocs.at("/paths/~1api~1external-agents/post/responses/200/content/*~1*/schema/$ref").asText())
                .isEqualTo("#/components/schemas/ExternalAgentDetailApiResponse");
        assertThat(apiDocs.at("/paths/~1api~1external-agents~1{adapterId}~1test/post/responses/200/content/*~1*/schema/$ref").asText())
                .isEqualTo("#/components/schemas/ExternalAgentTestApiResponse");
    }

    /**
     * 外部 Agent 和系统设置请求体中的可选字段不应被 OpenAPI 误标为必填。
     *
     * @throws Exception 请求和解析失败时抛出
     */
    @Test
    void masterDataRequestsKeepOptionalFieldsOptional() throws Exception {
        JsonNode apiDocs = readApiDocs();

        assertThat(requiredFields(apiDocs.at("/components/schemas/CreateExternalAgentRequest")))
                .containsExactlyInAnyOrder("adapterKey", "adapterType", "commandJson", "name");
        assertThat(requiredFields(apiDocs.at("/components/schemas/UpdateExternalAgentRequest")))
                .containsExactlyInAnyOrder("commandJson", "name");
        assertThat(requiredFields(apiDocs.at("/components/schemas/UpdateSettingsRequest")))
                .containsExactlyInAnyOrder("items");
    }

    /**
     * 读取 OpenAPI 文档。
     *
     * @return OpenAPI 文档树
     * @throws Exception 请求失败或解析失败时抛出
     */
    private JsonNode readApiDocs() throws Exception {
        String content = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(content);
    }

    /**
     * 提取 schema 的必填字段集合。
     *
     * @param schemaSchema schema 节点
     * @return 必填字段集合
     */
    private Set<String> requiredFields(JsonNode schemaSchema) {
        return objectMapper.convertValue(
                schemaSchema.path("required"),
                objectMapper.getTypeFactory().constructCollectionType(Set.class, String.class)
        );
    }
}
