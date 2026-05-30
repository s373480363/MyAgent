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
 * Schema OpenAPI 契约测试。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class SchemaOpenApiContractTests {

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
     * Schema 管理接口必须导出具体响应契约，而不是泛型统一响应占位符。
     *
     * @throws Exception 请求和解析失败时抛出
     */
    @Test
    void schemaApiExportsConcreteResponseContracts() throws Exception {
        JsonNode apiDocs = readApiDocs();

        assertThat(apiDocs.at("/paths/~1api~1schemas/get/responses/200/content/*~1*/schema/$ref").asText())
                .isEqualTo("#/components/schemas/SchemaPageApiResponse");
        assertThat(apiDocs.at("/paths/~1api~1schemas~1{schemaId}/get/responses/200/content/*~1*/schema/$ref").asText())
                .isEqualTo("#/components/schemas/SchemaDetailApiResponse");
        assertThat(apiDocs.at("/paths/~1api~1schemas/post/responses/200/content/*~1*/schema/$ref").asText())
                .isEqualTo("#/components/schemas/SchemaDetailApiResponse");
        assertThat(apiDocs.at("/paths/~1api~1schemas~1{schemaId}/put/responses/200/content/*~1*/schema/$ref").asText())
                .isEqualTo("#/components/schemas/SchemaDetailApiResponse");
    }

    /**
     * Schema 请求体中可选字段不能被 OpenAPI 误标为必填。
     *
     * @throws Exception 请求和解析失败时抛出
     */
    @Test
    void schemaRequestsKeepOptionalFieldsOptional() throws Exception {
        JsonNode apiDocs = readApiDocs();

        assertThat(requiredFields(apiDocs.at("/components/schemas/CreateSchemaRequest")))
                .containsExactlyInAnyOrder("createdFrom", "jsonSchema", "name", "schemaKey");
        assertThat(requiredFields(apiDocs.at("/components/schemas/CreateSchemaVersionRequest")))
                .containsExactlyInAnyOrder("jsonSchema", "name");
        assertThat(requiredFields(apiDocs.at("/components/schemas/UpdateSchemaDraftRequest")))
                .containsExactlyInAnyOrder("jsonSchema", "name");
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
