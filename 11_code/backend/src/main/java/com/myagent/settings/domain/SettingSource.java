package com.myagent.settings.domain;

/**
 * 系统设置当前值来源。
 */
public enum SettingSource {

    /**
     * 数据库覆盖值。
     */
    SYSTEM_SETTING,

    /**
     * 启动配置值。
     */
    APPLICATION_CONFIG
}
