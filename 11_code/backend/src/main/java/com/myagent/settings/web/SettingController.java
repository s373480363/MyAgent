package com.myagent.settings.web;

import com.myagent.settings.application.SettingApplicationService;
import com.myagent.settings.application.command.UpdateSettingsCommand;
import com.myagent.settings.web.dto.SettingsListApiResponse;
import com.myagent.settings.web.dto.UpdateSettingsRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统设置 REST 控制器。
 */
@Validated
@RestController
@RequestMapping("/api/settings")
@Tag(name = "Settings", description = "系统设置接口。")
public class SettingController {

    /**
     * 系统设置应用服务。
     */
    private final SettingApplicationService settingApplicationService;

    /**
     * 构造系统设置控制器。
     *
     * @param settingApplicationService 系统设置应用服务
     */
    public SettingController(SettingApplicationService settingApplicationService) {
        this.settingApplicationService = settingApplicationService;
    }

    /**
     * 查询系统设置。
     *
     * @return 系统设置列表
     */
    @GetMapping
    @Operation(summary = "查询系统设置", description = "只返回 V1 白名单中的 7 个系统设置项。", operationId = "getSettings")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(schema = @Schema(implementation = SettingsListApiResponse.class))
            )
    })
    public SettingsListApiResponse getSettings() {
        return new SettingsListApiResponse(true, settingApplicationService.getSettings(), null);
    }

    /**
     * 批量更新系统设置。
     *
     * @param request 批量更新请求
     * @return 更新后的系统设置列表
     */
    @PutMapping
    @Operation(summary = "批量更新系统设置", description = "仅允许更新 V1 白名单中的可编辑设置项。", operationId = "updateSettings")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(schema = @Schema(implementation = SettingsListApiResponse.class))
            )
    })
    public SettingsListApiResponse updateSettings(@Valid @RequestBody UpdateSettingsRequest request) {
        settingApplicationService.updateSettings(new UpdateSettingsCommand(
                request.getItems().stream()
                        .map(item -> new UpdateSettingsCommand.Item(
                                item.getSettingKey(),
                                item.getSettingValue(),
                                item.getValueType()
                        ))
                        .toList()
        ));
        return new SettingsListApiResponse(true, settingApplicationService.getSettings(), null);
    }
}
