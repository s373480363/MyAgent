package com.myagent.settings.web.dto;

import com.myagent.settings.domain.SettingValueType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 单个系统设置更新请求。
 */
public final class UpdateSettingsItemRequest {

    /**
     * 设置键。
     */
    @NotBlank(message = "settingKey 不能为空。")
    private String settingKey;

    /**
     * 设置值。
     */
    @NotNull(message = "settingValue 不能为空。")
    private String settingValue;

    /**
     * 值类型。
     */
    @NotNull(message = "valueType 不能为空。")
    private SettingValueType valueType;

    /**
     * 返回设置键。
     *
     * @return 设置键
     */
    public String getSettingKey() {
        return settingKey;
    }

    /**
     * 设置设置键。
     *
     * @param settingKey 设置键
     */
    public void setSettingKey(String settingKey) {
        this.settingKey = settingKey;
    }

    /**
     * 返回设置值。
     *
     * @return 设置值
     */
    public String getSettingValue() {
        return settingValue;
    }

    /**
     * 设置设置值。
     *
     * @param settingValue 设置值
     */
    public void setSettingValue(String settingValue) {
        this.settingValue = settingValue;
    }

    /**
     * 返回值类型。
     *
     * @return 值类型
     */
    public SettingValueType getValueType() {
        return valueType;
    }

    /**
     * 设置值类型。
     *
     * @param valueType 值类型
     */
    public void setValueType(SettingValueType valueType) {
        this.valueType = valueType;
    }
}
