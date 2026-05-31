package com.myagent.settings.domain;

import com.myagent.config.MyAgentSettingsProperties;

import java.util.function.Function;

/**
 * 单个系统设置白名单项。
 */
public final class SettingCatalogItem {

    /**
     * 设置键。
     */
    private final String settingKey;

    /**
     * 值类型。
     */
    private final SettingValueType valueType;

    /**
     * 是否可编辑。
     */
    private final boolean editable;

    /**
     * 中文说明。
     */
    private final String description;

    /**
     * 启动配置值解析器。
     */
    private final Function<MyAgentSettingsProperties, String> applicationValueResolver;

    /**
     * 构造白名单项。
     *
     * @param settingKey 设置键
     * @param valueType 值类型
     * @param editable 是否可编辑
     * @param description 中文说明
     * @param applicationValueResolver 启动配置值解析器
     */
    public SettingCatalogItem(
            String settingKey,
            SettingValueType valueType,
            boolean editable,
            String description,
            Function<MyAgentSettingsProperties, String> applicationValueResolver
    ) {
        this.settingKey = settingKey;
        this.valueType = valueType;
        this.editable = editable;
        this.description = description;
        this.applicationValueResolver = applicationValueResolver;
    }

    /**
     * 返回设置键。
     *
     * @return 设置键
     */
    public String getSettingKey() {
        return settingKey;
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
     * 返回是否可编辑。
     *
     * @return 是否可编辑
     */
    public boolean isEditable() {
        return editable;
    }

    /**
     * 返回中文说明。
     *
     * @return 中文说明
     */
    public String getDescription() {
        return description;
    }

    /**
     * 解析当前启动配置值。
     *
     * @param properties 启动配置对象
     * @return 当前启动配置值
     */
    public String resolveApplicationValue(MyAgentSettingsProperties properties) {
        return applicationValueResolver.apply(properties);
    }
}
