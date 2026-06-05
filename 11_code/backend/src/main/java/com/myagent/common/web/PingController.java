package com.myagent.common.web;

import com.myagent.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * 基线探活控制器。
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Baseline", description = "工程基线与探活接口。")
public class PingController {

    /**
     * 服务名称。
     */
    private final String serviceName;

    /**
     * 构造探活控制器。
     *
     * @param serviceName 当前服务名称
     */
    public PingController(@Value("${spring.application.name:agent-studio-api}") String serviceName) {
        this.serviceName = serviceName;
    }

    /**
     * 返回工程基线探活结果。
     *
     * @return 探活结果
     */
    @GetMapping("/ping")
    @Operation(summary = "探活接口", description = "返回当前后端基础服务信息。", operationId = "ping")
    public ApiResponse<PingResponse> ping() {
        // 在工程骨架阶段提供最小可用接口，便于验证统一响应结构与 OpenAPI 输出。
        PingResponse response = new PingResponse(serviceName, Instant.now(), "/v3/api-docs");
        return ApiResponse.success(response);
    }
}
