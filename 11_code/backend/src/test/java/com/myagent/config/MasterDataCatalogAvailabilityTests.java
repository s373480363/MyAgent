package com.myagent.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 主数据目录可用性集成测试。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class MasterDataCatalogAvailabilityTests {

    /**
     * PostgreSQL 测试容器。
     */
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("myagent_master_data_catalog_test")
                    .withUsername("myagent")
                    .withPassword("myagent");

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
     * 注册 PostgreSQL 测试容器配置。
     *
     * @param registry 动态属性注册器
     */
    @DynamicPropertySource
    static void registerPostgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.enabled", () -> true);
    }

    /**
     * JAVA_METHOD 目录必须内置至少一个正式可用的系统示例。
     *
     * @throws Exception 请求失败时抛出
     */
    @Test
    void javaMethodCatalogContainsSeededEntry() throws Exception {
        JsonNode response = readJson("/api/java-methods?keyword=java.sample.echo");

        assertThat(response.at("/data/total").asInt()).isGreaterThan(0);
        assertThat(response.at("/data/items/0/methodKey").asText()).isEqualTo("java.sample.echo");
        assertThat(response.at("/data/items/0/beanName").asText()).isEqualTo("systemEchoJavaMethod");
    }

    /**
     * TOOL 目录必须内置至少一个正式可用的系统示例。
     *
     * @throws Exception 请求失败时抛出
     */
    @Test
    void toolCatalogContainsSeededEntry() throws Exception {
        JsonNode response = readJson("/api/tools?keyword=tool.sample.echo");

        assertThat(response.at("/data/total").asInt()).isGreaterThan(0);
        assertThat(response.at("/data/items/0/toolKey").asText()).isEqualTo("tool.sample.echo");
        assertThat(response.at("/data/items/0/executorType").asText()).isEqualTo("ECHO");
    }

    /**
     * 读取 JSON 响应。
     *
     * @param path 请求路径
     * @return 响应 JSON
     * @throws Exception 请求失败时抛出
     */
    private JsonNode readJson(String path) throws Exception {
        String content = mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(content);
    }
}
