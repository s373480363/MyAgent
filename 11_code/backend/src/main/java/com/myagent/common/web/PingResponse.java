package com.myagent.common.web;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * 基线探活响应对象。
 */
@Schema(name = "PingResponse", description = "基线探活响应对象。")
public final class PingResponse {

    /**
     * 服务名称。
     */
    @Schema(description = "服务名称。", example = "agent-studio-api")
    private final String serviceName;

    /**
     * 服务端当前时间。
     */
    @Schema(description = "服务端当前时间。")
    private final Instant serverTime;

    /**
     * OpenAPI 文档路径。
     */
    @Schema(description = "OpenAPI 文档路径。", example = "/v3/api-docs")
    private final String openApiPath;

    /**
     * 构造探活响应对象。
     *
     * @param serviceName 服务名称
     * @param serverTime 服务端当前时间
     * @param openApiPath OpenAPI 文档路径
     */
    public PingResponse(String serviceName, Instant serverTime, String openApiPath) {
        this.serviceName = serviceName;
        this.serverTime = serverTime;
        this.openApiPath = openApiPath;
    }

    /**
     * 返回服务名称。
     *
     * @return 服务名称
     */
    public String getServiceName() {
        return serviceName;
    }

    /**
     * 返回服务端当前时间。
     *
     * @return 服务端当前时间
     */
    public Instant getServerTime() {
        return serverTime;
    }

    /**
     * 返回 OpenAPI 文档路径。
     *
     * @return OpenAPI 文档路径
     */
    public String getOpenApiPath() {
        return openApiPath;
    }
}
