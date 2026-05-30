package com.myagent.config;

import org.junit.jupiter.api.Test;
import io.swagger.v3.oas.models.OpenAPI;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OpenAPI 契约测试。
 */
class OpenApiContractTests {

    /**
     * 验证 OpenAPI 基线配置能够装配公共 schema。
     */
    @Test
    void openApiConfigRegistersCommonSchemas() {
        OpenAPI openAPI = new OpenAPI();
        new OpenApiConfig().myAgentOpenApiCustomizer().customise(openAPI);

        assertThat(openAPI.getComponents()).isNotNull();
        assertThat(openAPI.getComponents().getSchemas()).containsKeys("ApiResponse", "ApiError", "PageResponse");
    }
}
