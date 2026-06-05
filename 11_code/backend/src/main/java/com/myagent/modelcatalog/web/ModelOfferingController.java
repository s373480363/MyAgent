package com.myagent.modelcatalog.web;

import com.myagent.common.api.PageResponse;
import com.myagent.common.domain.EnableStatus;
import com.myagent.modelcatalog.application.ModelCatalogApplicationService;
import com.myagent.modelcatalog.application.command.ChangeModelOfferingStatusCommand;
import com.myagent.modelcatalog.application.command.CreateModelOfferingCommand;
import com.myagent.modelcatalog.application.command.UpdateModelOfferingCommand;
import com.myagent.modelcatalog.application.query.ListModelOfferingsQuery;
import com.myagent.modelcatalog.web.dto.ChangeModelOfferingStatusRequest;
import com.myagent.modelcatalog.web.dto.CreateModelOfferingRequest;
import com.myagent.modelcatalog.web.dto.ModelOfferingBatchApiResponse;
import com.myagent.modelcatalog.web.dto.ModelOfferingDetailApiResponse;
import com.myagent.modelcatalog.web.dto.ModelOfferingPageApiResponse;
import com.myagent.modelcatalog.web.dto.UpdateModelOfferingRequest;
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

import java.util.List;

/**
 * 模型供应项 REST 控制器。
 */
@Validated
@RestController
@RequestMapping("/api/model-offerings")
@Tag(name = "ModelOfferings", description = "模型供应项目录接口。")
public class ModelOfferingController {

    /**
     * 模型目录应用服务。
     */
    private final ModelCatalogApplicationService modelCatalogApplicationService;

    /**
     * 构造控制器。
     *
     * @param modelCatalogApplicationService 模型目录应用服务
     */
    public ModelOfferingController(ModelCatalogApplicationService modelCatalogApplicationService) {
        this.modelCatalogApplicationService = modelCatalogApplicationService;
    }

    /**
     * 查询模型供应项列表。
     *
     * @param page 页码
     * @param pageSize 每页条数
     * @param providerKey 供应商标识
     * @param keyword 关键词
     * @param status 状态
     * @return 分页结果
     */
    @GetMapping
    @Operation(summary = "查询模型供应项列表", description = "分页查询模型供应项，默认只返回当前可选项。", operationId = "listModelOfferings")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(schema = @Schema(implementation = ModelOfferingPageApiResponse.class))
            )
    })
    public ModelOfferingPageApiResponse listModelOfferings(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) long pageSize,
            @RequestParam(required = false) String providerKey,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) EnableStatus status
    ) {
        return new ModelOfferingPageApiResponse(
                true,
                PageResponse.from(modelCatalogApplicationService.listModelOfferings(
                        new ListModelOfferingsQuery(page, pageSize, providerKey, keyword, status)
                )),
                null
        );
    }

    /**
     * 按键查询模型供应项。
     *
     * @param offeringKey 供应项标识
     * @return 详情
     */
    @GetMapping("/{offeringKey}")
    @Operation(summary = "按键查询模型供应项", description = "按 offeringKey 返回模型供应项详情，可用于编辑页当前绑定值回填。", operationId = "getModelOffering")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(schema = @Schema(implementation = ModelOfferingDetailApiResponse.class))
            )
    })
    public ModelOfferingDetailApiResponse getModelOffering(@PathVariable String offeringKey) {
        return new ModelOfferingDetailApiResponse(
                true,
                modelCatalogApplicationService.getModelOffering(offeringKey),
                null
        );
    }

    /**
     * 按键批量查询模型供应项。
     *
     * @param offeringKeys 供应项键列表
     * @return 查询结果
     */
    @GetMapping("/by-keys")
    @Operation(summary = "按键批量查询模型供应项", description = "按 offeringKey 批量回填当前绑定的模型供应项。", operationId = "getModelOfferingsByKeys")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(schema = @Schema(implementation = ModelOfferingBatchApiResponse.class))
            )
    })
    public ModelOfferingBatchApiResponse getModelOfferingsByKeys(@RequestParam List<String> offeringKeys) {
        return new ModelOfferingBatchApiResponse(
                true,
                modelCatalogApplicationService.getModelOfferingsByKeys(offeringKeys),
                null
        );
    }

    /**
     * 创建模型供应项。
     *
     * @param request 创建请求
     * @return 详情
     */
    @PostMapping
    @Operation(summary = "创建模型供应项", description = "创建供应商下的模型供应项入口。", operationId = "createModelOffering")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(schema = @Schema(implementation = ModelOfferingDetailApiResponse.class))
            )
    })
    public ModelOfferingDetailApiResponse createModelOffering(@Valid @RequestBody CreateModelOfferingRequest request) {
        return new ModelOfferingDetailApiResponse(
                true,
                modelCatalogApplicationService.createModelOffering(new CreateModelOfferingCommand(
                        request.getOfferingKey(),
                        request.getProviderKey(),
                        request.getModelKey(),
                        request.getDisplayName(),
                        request.getUpstreamModelName(),
                        request.getDefaultTemperature(),
                        request.getDescription()
                )),
                null
        );
    }

    /**
     * 更新模型供应项。
     *
     * @param offeringId 供应项主键
     * @param request 更新请求
     * @return 详情
     */
    @PutMapping("/{offeringId}")
    @Operation(summary = "更新模型供应项", description = "更新模型供应项普通字段。", operationId = "updateModelOffering")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(schema = @Schema(implementation = ModelOfferingDetailApiResponse.class))
            )
    })
    public ModelOfferingDetailApiResponse updateModelOffering(
            @PathVariable @Min(1) long offeringId,
            @Valid @RequestBody UpdateModelOfferingRequest request
    ) {
        return new ModelOfferingDetailApiResponse(
                true,
                modelCatalogApplicationService.updateModelOffering(new UpdateModelOfferingCommand(
                        offeringId,
                        request.getProviderKey(),
                        request.getModelKey(),
                        request.getDisplayName(),
                        request.getUpstreamModelName(),
                        request.getDefaultTemperature(),
                        request.getDescription()
                )),
                null
        );
    }

    /**
     * 修改模型供应项状态。
     *
     * @param offeringId 供应项主键
     * @param request 状态请求
     * @return 最新详情
     */
    @PutMapping("/{offeringId}/status")
    @Operation(summary = "修改模型供应项状态", description = "启用或停用模型供应项。", operationId = "changeModelOfferingStatus")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(schema = @Schema(implementation = ModelOfferingDetailApiResponse.class))
            )
    })
    public ModelOfferingDetailApiResponse changeModelOfferingStatus(
            @PathVariable @Min(1) long offeringId,
            @Valid @RequestBody ChangeModelOfferingStatusRequest request
    ) {
        return new ModelOfferingDetailApiResponse(
                true,
                modelCatalogApplicationService.changeModelOfferingStatus(
                        new ChangeModelOfferingStatusCommand(offeringId, request.getStatus())
                ),
                null
        );
    }
}
