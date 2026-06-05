package com.myagent.modelcatalog.web;

import com.myagent.common.api.PageResponse;
import com.myagent.common.domain.EnableStatus;
import com.myagent.modelcatalog.application.ModelCatalogApplicationService;
import com.myagent.modelcatalog.application.command.ChangeModelProviderStatusCommand;
import com.myagent.modelcatalog.application.command.CreateModelProviderCommand;
import com.myagent.modelcatalog.application.command.TestModelProviderCommand;
import com.myagent.modelcatalog.application.command.UpdateModelProviderCommand;
import com.myagent.modelcatalog.application.command.UpdateModelProviderSecretsCommand;
import com.myagent.modelcatalog.application.query.GetModelProviderQuery;
import com.myagent.modelcatalog.application.query.ListModelProvidersQuery;
import com.myagent.modelcatalog.web.dto.ChangeModelProviderStatusRequest;
import com.myagent.modelcatalog.web.dto.CreateModelProviderRequest;
import com.myagent.modelcatalog.web.dto.ModelProviderDetailApiResponse;
import com.myagent.modelcatalog.web.dto.ModelProviderPageApiResponse;
import com.myagent.modelcatalog.web.dto.ModelProviderTestApiResponse;
import com.myagent.modelcatalog.web.dto.TestModelProviderRequest;
import com.myagent.modelcatalog.web.dto.UpdateModelProviderRequest;
import com.myagent.modelcatalog.web.dto.UpdateModelProviderSecretsRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
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
 * 模型供应商 REST 控制器。
 */
@Validated
@RestController
@RequestMapping("/api/model-providers")
@Tag(name = "ModelProviders", description = "模型供应商目录接口。")
public class ModelProviderController {

    /**
     * 模型目录应用服务。
     */
    private final ModelCatalogApplicationService modelCatalogApplicationService;

    /**
     * 构造控制器。
     *
     * @param modelCatalogApplicationService 模型目录应用服务
     */
    public ModelProviderController(ModelCatalogApplicationService modelCatalogApplicationService) {
        this.modelCatalogApplicationService = modelCatalogApplicationService;
    }

    /**
     * 查询模型供应商列表。
     *
     * @param page 页码
     * @param pageSize 每页条数
     * @param keyword 关键词
     * @param status 状态
     * @return 分页结果
     */
    @GetMapping
    @Operation(summary = "查询模型供应商列表", description = "按分页、关键词和状态筛选模型供应商。", operationId = "listModelProviders")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(schema = @Schema(implementation = ModelProviderPageApiResponse.class))
            )
    })
    public ModelProviderPageApiResponse listModelProviders(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) EnableStatus status
    ) {
        return new ModelProviderPageApiResponse(
                true,
                PageResponse.from(modelCatalogApplicationService.listModelProviders(
                        new ListModelProvidersQuery(page, pageSize, keyword, status)
                )),
                null
        );
    }

    /**
     * 查询模型供应商详情。
     *
     * @param providerId 供应商主键
     * @return 详情
     */
    @GetMapping("/{providerId}")
    @Operation(summary = "查询模型供应商详情", description = "返回模型供应商安全详情，不回显 API Key 明文。", operationId = "getModelProvider")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(schema = @Schema(implementation = ModelProviderDetailApiResponse.class))
            )
    })
    public ModelProviderDetailApiResponse getModelProvider(@PathVariable @Min(1) long providerId) {
        return new ModelProviderDetailApiResponse(
                true,
                modelCatalogApplicationService.getModelProvider(new GetModelProviderQuery(providerId)),
                null
        );
    }

    /**
     * 创建模型供应商。
     *
     * @param request 创建请求
     * @return 详情
     */
    @PostMapping
    @Operation(summary = "创建模型供应商", description = "创建模型供应商并可选写入初始 API Key。", operationId = "createModelProvider")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(schema = @Schema(implementation = ModelProviderDetailApiResponse.class))
            )
    })
    public ModelProviderDetailApiResponse createModelProvider(@Valid @RequestBody CreateModelProviderRequest request) {
        return new ModelProviderDetailApiResponse(
                true,
                modelCatalogApplicationService.createModelProvider(new CreateModelProviderCommand(
                        request.getProviderKey(),
                        request.getName(),
                        request.getProviderType(),
                        request.getBaseUrl(),
                        request.getApiKey(),
                        request.getDescription()
                )),
                null
        );
    }

    /**
     * 更新模型供应商普通字段。
     *
     * @param providerId 供应商主键
     * @param request 更新请求
     * @return 详情
     */
    @PutMapping("/{providerId}")
    @Operation(summary = "更新模型供应商", description = "只更新名称、描述和 Base URL 等非敏感字段。", operationId = "updateModelProvider")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(schema = @Schema(implementation = ModelProviderDetailApiResponse.class))
            )
    })
    public ModelProviderDetailApiResponse updateModelProvider(
            @PathVariable @Min(1) long providerId,
            @Valid @RequestBody UpdateModelProviderRequest request
    ) {
        return new ModelProviderDetailApiResponse(
                true,
                modelCatalogApplicationService.updateModelProvider(new UpdateModelProviderCommand(
                        providerId,
                        request.getName(),
                        request.getBaseUrl(),
                        request.getDescription()
                )),
                null
        );
    }

    /**
     * 更新模型供应商密钥。
     *
     * @param providerId 供应商主键
     * @param request 更新请求
     * @return 详情
     */
    @PutMapping("/{providerId}/secrets")
    @Operation(summary = "更新模型供应商密钥", description = "替换或清空 API Key，不通过普通更新接口处理敏感字段。", operationId = "updateModelProviderSecrets")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(schema = @Schema(implementation = ModelProviderDetailApiResponse.class))
            )
    })
    public ModelProviderDetailApiResponse updateModelProviderSecrets(
            @PathVariable @Min(1) long providerId,
            @Valid @RequestBody UpdateModelProviderSecretsRequest request
    ) {
        return new ModelProviderDetailApiResponse(
                true,
                modelCatalogApplicationService.updateModelProviderSecrets(new UpdateModelProviderSecretsCommand(
                        providerId,
                        request.getApiKey(),
                        Boolean.TRUE.equals(request.getClearApiKey())
                )),
                null
        );
    }

    /**
     * 修改模型供应商状态。
     *
     * @param providerId 供应商主键
     * @param request 状态请求
     * @return 最新详情
     */
    @PutMapping("/{providerId}/status")
    @Operation(summary = "修改模型供应商状态", description = "启用或停用模型供应商。", operationId = "changeModelProviderStatus")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(schema = @Schema(implementation = ModelProviderDetailApiResponse.class))
            )
    })
    public ModelProviderDetailApiResponse changeModelProviderStatus(
            @PathVariable @Min(1) long providerId,
            @Valid @RequestBody ChangeModelProviderStatusRequest request
    ) {
        modelCatalogApplicationService.changeModelProviderStatus(new ChangeModelProviderStatusCommand(providerId, request.getStatus()));
        return new ModelProviderDetailApiResponse(
                true,
                modelCatalogApplicationService.getModelProvider(new GetModelProviderQuery(providerId)),
                null
        );
    }

    /**
     * 测试模型供应商连接。
     *
     * @param providerId 供应商主键
     * @param request 测试请求
     * @return 测试结果
     */
    @PostMapping("/{providerId}/test")
    @Operation(summary = "测试模型供应商连接", description = "使用指定供应项发起轻量连接测试，不创建正式 AgentRun。", operationId = "testModelProvider")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(schema = @Schema(implementation = ModelProviderTestApiResponse.class))
            )
    })
    public ModelProviderTestApiResponse testModelProvider(
            @PathVariable @Min(1) long providerId,
            @Valid @RequestBody TestModelProviderRequest request
    ) {
        return new ModelProviderTestApiResponse(
                true,
                modelCatalogApplicationService.testModelProvider(new TestModelProviderCommand(
                        providerId,
                        request.getOfferingKey(),
                        request.getPrompt()
                )),
                null
        );
    }
}
