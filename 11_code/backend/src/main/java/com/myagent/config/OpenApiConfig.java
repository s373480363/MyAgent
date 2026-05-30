package com.myagent.config;

import com.myagent.common.api.ApiError;
import com.myagent.common.api.ApiResponse;
import com.myagent.common.api.PageResponse;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ResolvedSchema;
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
     * 创建统一 OpenAPI 文档元信息。
     *
     * @return OpenAPI 对象
     */
    @Bean
    public OpenAPI myAgentOpenApi() {
        // 统一输出平台级契约元信息，后续所有接口都在同一份 OpenAPI 文档中汇总。
        return new OpenAPI().info(new Info()
                .title("MyAgent API")
                .version("v1")
                .description("MyAgent V1 后端 REST 接口契约。"));
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
                openApi.setComponents(new io.swagger.v3.oas.models.Components());
            }
            // 公共模型不一定都被当前阶段的 Controller 直接引用，因此需要显式注册，避免前端生成类型遗漏基线契约。
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
                new io.swagger.v3.core.converter.AnnotatedType(modelType)
        );
        if (resolvedSchema != null && resolvedSchema.schema != null) {
            openApi.getComponents().addSchemas(schemaName, resolvedSchema.schema);
        }
    }
}
