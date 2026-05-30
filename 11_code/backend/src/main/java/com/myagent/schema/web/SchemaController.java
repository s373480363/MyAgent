package com.myagent.schema.web;

import com.myagent.common.error.BizException;
import com.myagent.common.error.ErrorCode;
import com.myagent.schema.application.SchemaApplicationService;
import com.myagent.schema.application.command.CreateSchemaCommand;
import com.myagent.schema.application.command.CreateSchemaVersionCommand;
import com.myagent.schema.application.command.UpdateSchemaDraftCommand;
import com.myagent.schema.application.query.GetSchemaQuery;
import com.myagent.schema.application.query.ListSchemasQuery;
import com.myagent.schema.application.result.SchemaDetailResult;
import com.myagent.schema.application.result.SchemaPageResponse;
import com.myagent.schema.domain.SchemaCreatedFrom;
import com.myagent.schema.domain.SchemaStatus;
import com.myagent.schema.web.dto.CreateSchemaRequest;
import com.myagent.schema.web.dto.CreateSchemaVersionRequest;
import com.myagent.schema.web.dto.SchemaDetailApiResponse;
import com.myagent.schema.web.dto.SchemaPageApiResponse;
import com.myagent.schema.web.dto.UpdateSchemaDraftRequest;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Schema 管理 REST 控制器。
 */
@Validated
@RestController
@RequestMapping("/api/schemas")
@Tag(name = "Schemas", description = "Schema 管理接口。")
public class SchemaController {

    /**
     * Schema 应用服务。
     */
    private final SchemaApplicationService schemaApplicationService;

    /**
     * 构造 Schema 控制器。
     *
     * @param schemaApplicationService Schema 应用服务
     */
    public SchemaController(SchemaApplicationService schemaApplicationService) {
        this.schemaApplicationService = schemaApplicationService;
    }

    /**
     * 查询 Schema 列表。
     *
     * @param page 页码
     * @param pageSize 每页条数
     * @param keyword 关键词
     * @param status 生命周期状态
     * @param createdFrom 来源
     * @return Schema 分页列表
     */
    @GetMapping
    @Operation(summary = "查询 Schema 列表", description = "按分页、状态、来源和关键词查询 Schema。", operationId = "listSchemas")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(schema = @Schema(implementation = SchemaPageApiResponse.class))
            )
    })
    public SchemaPageApiResponse listSchemas(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) SchemaStatus status,
            @RequestParam(required = false) SchemaCreatedFrom createdFrom
    ) {
        SchemaPageResponse response = SchemaPageResponse.from(schemaApplicationService.listSchemas(
                new ListSchemasQuery(page, pageSize, keyword, status, createdFrom)
        ));
        return new SchemaPageApiResponse(true, response, null);
    }

    /**
     * 创建 Schema。
     *
     * @param request 创建请求
     * @return Schema 详情
     */
    @PostMapping
    @Operation(summary = "创建 Schema", description = "创建新的 Schema 系列或基于来源 Schema 创建草稿版本。", operationId = "createSchema")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(schema = @Schema(implementation = SchemaDetailApiResponse.class))
            )
    })
    public SchemaDetailApiResponse createSchema(@Valid @RequestBody CreateSchemaRequest request) {
        validateCreateRequest(request);
        SchemaDetailResult response = schemaApplicationService.createSchema(new CreateSchemaCommand(
                request.getSchemaKey(),
                request.getName(),
                request.getDescription(),
                request.getJsonSchema(),
                request.getJavaType(),
                request.getCreatedFrom(),
                request.getSourceSchemaId()
        ));
        return new SchemaDetailApiResponse(true, response, null);
    }

    /**
     * 更新 Schema 草稿。
     *
     * @param schemaId Schema 主键
     * @param request 更新请求
     * @return Schema 详情
     */
    @PutMapping("/{schemaId}")
    @Operation(summary = "更新 Schema 草稿", description = "仅允许更新 status=DRAFT 且 locked=false 的 Schema。", operationId = "updateSchemaDraft")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(schema = @Schema(implementation = SchemaDetailApiResponse.class))
            )
    })
    public SchemaDetailApiResponse updateSchemaDraft(
            @PathVariable @Min(1) long schemaId,
            @Valid @RequestBody UpdateSchemaDraftRequest request
    ) {
        SchemaDetailResult response = schemaApplicationService.updateSchemaDraft(new UpdateSchemaDraftCommand(
                schemaId,
                request.getName(),
                request.getDescription(),
                request.getJsonSchema(),
                request.getJavaType()
        ));
        return new SchemaDetailApiResponse(true, response, null);
    }

    /**
     * 查询 Schema 详情。
     *
     * @param schemaId Schema 主键
     * @return Schema 详情
     */
    @GetMapping("/{schemaId}")
    @Operation(summary = "查询 Schema 详情", description = "查询 Schema 基础信息、版本、JSON Schema 内容和锁定状态。", operationId = "getSchema")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(schema = @Schema(implementation = SchemaDetailApiResponse.class))
            )
    })
    public SchemaDetailApiResponse getSchema(@PathVariable @Min(1) long schemaId) {
        return new SchemaDetailApiResponse(true, schemaApplicationService.getSchema(new GetSchemaQuery(schemaId)), null);
    }

    /**
     * 基于旧版本创建新版本。
     *
     * @param schemaId 来源 Schema 主键
     * @param request 创建版本请求
     * @return 新版本 Schema 详情
     */
    @PostMapping("/{schemaId}/versions")
    @Operation(summary = "创建 Schema 新版本", description = "基于旧版本创建同一 schemaKey 的下一整数 DRAFT 版本。", operationId = "createSchemaVersion")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(schema = @Schema(implementation = SchemaDetailApiResponse.class))
            )
    })
    public SchemaDetailApiResponse createSchemaVersion(
            @PathVariable @Min(1) long schemaId,
            @Valid @RequestBody CreateSchemaVersionRequest request
    ) {
        SchemaDetailResult response = schemaApplicationService.createSchemaVersion(new CreateSchemaVersionCommand(
                schemaId,
                request.getName(),
                request.getDescription(),
                request.getJsonSchema(),
                request.getJavaType()
        ));
        return new SchemaDetailApiResponse(true, response, null);
    }

    /**
     * 校验创建请求。
     *
     * @param request 创建请求
     */
    private void validateCreateRequest(CreateSchemaRequest request) {
        if (request.getSourceSchemaId() != null && request.getSourceSchemaId() <= 0) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "来源 Schema 主键必须大于 0。");
        }
    }
}
