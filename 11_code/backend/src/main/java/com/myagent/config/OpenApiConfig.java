package com.myagent.config;

import com.myagent.common.api.ApiError;
import com.myagent.common.api.ApiResponse;
import com.myagent.common.api.PageResponse;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ResolvedSchema;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 基线配置。
 */
@Configuration
public class OpenApiConfig {

    /**
     * 创建统一 OpenAPI 元信息。
     *
     * @return OpenAPI 对象
     */
    @Bean
    public OpenAPI myAgentOpenApi() {
        return new OpenAPI().info(new Info()
                .title("agent-studio-api")
                .version("v1")
                .description("Agent Studio V1 后端 REST 接口契约。"));
    }

    /**
     * 将公共模型显式注册进 OpenAPI 组件。
     *
     * @return OpenAPI 自定义器
     */
    @Bean
    public OpenApiCustomizer myAgentOpenApiCustomizer() {
        return openApi -> {
            if (openApi.getComponents() == null) {
                openApi.setComponents(new Components());
            }
            registerSchema(openApi, ApiResponse.class, "ApiResponse");
            registerSchema(openApi, ApiError.class, "ApiError");
            registerSchema(openApi, PageResponse.class, "PageResponse");
        };
    }

    /**
     * 将指定模型注册为 OpenAPI 组件。
     *
     * @param openApi OpenAPI 对象
     * @param modelType Java 模型类型
     * @param schemaName 组件名称
     */
    private void registerSchema(OpenAPI openApi, Class<?> modelType, String schemaName) {
        ResolvedSchema resolvedSchema = ModelConverters.getInstance().resolveAsResolvedSchema(
                new AnnotatedType(modelType)
        );
        if (resolvedSchema != null && resolvedSchema.schema != null) {
            openApi.getComponents().addSchemas(schemaName, resolvedSchema.schema);
        }
    }
}
