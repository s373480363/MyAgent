package com.myagent.settings.repository;

import com.myagent.settings.domain.SettingValueType;

import java.time.Instant;

/**
 * 系统设置持久化记录。
 *
 * @param settingKey 设置键
 * @param settingValue 设置值
 * @param valueType 值类型
 * @param description 中文说明
 * @param editable 是否可编辑
 * @param updatedAt 更新时间
 */
public record SystemSettingRecord(
        String settingKey,
        String settingValue,
        SettingValueType valueType,
        String description,
        boolean editable,
        Instant updatedAt
) {
}
