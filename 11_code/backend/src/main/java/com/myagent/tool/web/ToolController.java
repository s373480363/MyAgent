package com.myagent.tool.web;

import com.myagent.common.api.PageResponse;
import com.myagent.common.domain.EnableStatus;
import com.myagent.tool.application.ToolApplicationService;
import com.myagent.tool.application.query.GetToolQuery;
import com.myagent.tool.application.query.ListToolsQuery;
import com.myagent.tool.web.dto.ToolDetailApiResponse;
import com.myagent.tool.web.dto.ToolPageApiResponse;
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
 * 工具 REST 控制器。
 */
@Validated
@RestController
@RequestMapping("/api/tools")
@Tag(name = "Tools", description = "工具目录接口。")
public class ToolController {

    /**
     * 工具应用服务。
     */
    private final ToolApplicationService toolApplicationService;

    /**
     * 构造工具控制器。
     *
     * @param toolApplicationService 工具应用服务
     */
    public ToolController(ToolApplicationService toolApplicationService) {
        this.toolApplicationService = toolApplicationService;
    }

    /**
     * 查询工具列表。
     *
     * @param page 页码
     * @param pageSize 每页条数
     * @param keyword 关键词
     * @param status 状态
     * @param executorType 执行器类型
     * @return 分页结果
     */
    @GetMapping
    @Operation(summary = "查询工具列表", description = "按分页、关键词、状态和执行器类型筛选工具目录。", operationId = "listTools")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(schema = @Schema(implementation = ToolPageApiResponse.class))
            )
    })
    public ToolPageApiResponse listTools(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) EnableStatus status,
            @RequestParam(required = false) String executorType
    ) {
        return new ToolPageApiResponse(
                true,
                PageResponse.from(toolApplicationService.listTools(
                        new ListToolsQuery(page, pageSize, keyword, status, executorType)
                )),
                null
        );
    }

    /**
     * 查询工具详情。
     *
     * @param toolId 工具主键
     * @return 详情结果
     */
    @GetMapping("/{toolId}")
    @Operation(summary = "查询工具详情", description = "查询单个工具定义详情。", operationId = "getTool")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(schema = @Schema(implementation = ToolDetailApiResponse.class))
            )
    })
    public ToolDetailApiResponse getTool(@PathVariable @Min(1) long toolId) {
        return new ToolDetailApiResponse(
                true,
                toolApplicationService.getTool(new GetToolQuery(toolId)),
                null
        );
    }
}
