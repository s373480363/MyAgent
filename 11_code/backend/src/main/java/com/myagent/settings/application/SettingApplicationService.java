package com.myagent.settings.application;

import com.myagent.settings.application.command.UpdateSettingsCommand;
import com.myagent.settings.application.result.SettingItemResult;

import java.util.List;

/**
 * 系统设置应用服务。
 */
public interface SettingApplicationService {

    /**
     * 读取系统设置列表。
     *
     * @return 系统设置列表
     */
    List<SettingItemResult> getSettings();

    /**
     * 批量更新系统设置。
     *
     * @param command 批量更新命令
     */
    void updateSettings(UpdateSettingsCommand command);
}
