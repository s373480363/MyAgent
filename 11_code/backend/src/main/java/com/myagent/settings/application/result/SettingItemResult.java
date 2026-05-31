package com.myagent.settings.application.result;

import com.myagent.settings.domain.SettingSource;
import com.myagent.settings.domain.SettingValueType;

/**
 * 单个系统设置返回结果。
 *
 * @param settingKey 设置键
 * @param settingValue 当前生效值
 * @param valueType 值类型
 * @param editable 是否可编辑
 * @param description 中文说明
 * @param source 当前值来源
 */
public record SettingItemResult(
        String settingKey,
        String settingValue,
        SettingValueType valueType,
        boolean editable,
        String description,
        SettingSource source
) {
}
