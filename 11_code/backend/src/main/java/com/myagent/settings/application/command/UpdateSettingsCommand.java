package com.myagent.settings.application.command;

import com.myagent.settings.domain.SettingValueType;

import java.util.List;

/**
 * 批量更新系统设置命令。
 *
 * @param items 待更新设置项
 */
public record UpdateSettingsCommand(List<Item> items) {

    /**
     * 单个待更新设置项。
     *
     * @param settingKey 设置键
     * @param settingValue 设置值
     * @param valueType 值类型
     */
    public record Item(
            String settingKey,
            String settingValue,
            SettingValueType valueType
    ) {
    }
}
