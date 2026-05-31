package com.myagent.method.web;

import com.myagent.common.api.PageResponse;
import com.myagent.common.domain.EnableStatus;
import com.myagent.method.application.JavaMethodApplicationService;
import com.myagent.method.application.query.GetJavaMethodQuery;
import com.myagent.method.application.query.ListJavaMethodsQuery;
import com.myagent.method.web.dto.JavaMethodDetailApiResponse;
import com.myagent.method.web.dto.JavaMethodPageApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Java 方法 REST 控制器。
 */
@Validated
@RestController
@RequestMapping("/api/java-methods")
@Tag(name = "JavaMethods", description = "Java 方法目录接口。")
public class JavaMethodController {

    /**
     * Java 方法应用服务。
     */
    private final JavaMethodApplicationService javaMethodApplicationService;

    /**
     * 构造 Java 方法控制器。
     *
     * @param javaMethodApplicationService Java 方法应用服务
     */
    public JavaMethodController(JavaMethodApplicationService javaMethodApplicationService) {
        this.javaMethodApplicationService = javaMethodApplicationService;
    }

    /**
     * 查询 Java 方法列表。
     *
     * @param page 页码
     * @param pageSize 每页条数
     * @param keyword 关键词
     * @param status 状态
     * @return 分页结果
     */
    @GetMapping
    @Operation(summary = "查询 Java 方法列表", description = "按分页、关键词和状态筛选已注册 Java 方法。", operationId = "listJavaMethods")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(schema = @Schema(implementation = JavaMethodPageApiResponse.class))
            )
    })
    public JavaMethodPageApiResponse listJavaMethods(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) EnableStatus status
    ) {
        return new JavaMethodPageApiResponse(
                true,
                PageResponse.from(javaMethodApplicationService.listJavaMethods(
                        new ListJavaMethodsQuery(page, pageSize, keyword, status)
                )),
                null
        );
    }

    /**
     * 查询 Java 方法详情。
     *
     * @param methodId 方法主键
     * @return 详情结果
     */
    @GetMapping("/{methodId}")
    @Operation(summary = "查询 Java 方法详情", description = "查询单个已注册 Java 方法详情。", operationId = "getJavaMethod")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(schema = @Schema(implementation = JavaMethodDetailApiResponse.class))
            )
    })
    public JavaMethodDetailApiResponse getJavaMethod(@PathVariable @Min(1) long methodId) {
        return new JavaMethodDetailApiResponse(
                true,
                javaMethodApplicationService.getJavaMethod(new GetJavaMethodQuery(methodId)),
                null
        );
    }
}
