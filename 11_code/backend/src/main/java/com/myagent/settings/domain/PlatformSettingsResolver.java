package com.myagent.settings.domain;

import com.myagent.config.MyAgentSettingsProperties;
import com.myagent.settings.repository.SystemSettingRecord;
import com.myagent.settings.repository.SystemSettingRepository;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 平台白名单设置读取器。
 */
@Component
public class PlatformSettingsResolver {

    /**
     * 默认模型设置键。
     */
    public static final String DEFAULT_MODEL_KEY = "myagent.openai.default-model";

    /**
     * Agent 默认总超时设置键。
     */
    public static final String DEFAULT_AGENT_TIMEOUT_KEY = "myagent.runtime.default-agent-timeout-seconds";

    /**
     * 默认最大步数设置键。
     */
    public static final String DEFAULT_MAX_STEPS_KEY = "myagent.runtime.default-max-steps";

    /**
     * 默认最大 Agent 调用深度设置键。
     */
    public static final String DEFAULT_MAX_AGENT_CALL_DEPTH_KEY = "myagent.runtime.default-max-agent-call-depth";

    /**
     * 系统设置仓储。
     */
    private final SystemSettingRepository systemSettingRepository;

    /**
     * 启动配置。
     */
    private final MyAgentSettingsProperties settingsProperties;

    /**
     * 构造平台设置读取器。
     *
     * @param systemSettingRepository 系统设置仓储
     * @param settingsProperties 启动配置
     */
    public PlatformSettingsResolver(
            SystemSettingRepository systemSettingRepository,
            MyAgentSettingsProperties settingsProperties
    ) {
        this.systemSettingRepository = systemSettingRepository;
        this.settingsProperties = settingsProperties;
    }

    /**
     * 返回默认模型。
     *
     * @return 默认模型
     */
    public String resolveDefaultModel() {
        Map<String, SystemSettingRecord> records = systemSettingRepository.findByKeys(java.util.List.of(DEFAULT_MODEL_KEY));
        SystemSettingRecord record = records.get(DEFAULT_MODEL_KEY);
        if (record != null && record.settingValue() != null && !record.settingValue().isBlank()) {
            return record.settingValue().trim();
        }
        return settingsProperties.getOpenai().getDefaultModel();
    }

    /**
     * 返回 Agent 默认总超时。
     *
     * @return 默认总超时
     */
    public int resolveDefaultAgentTimeoutSeconds() {
        return resolvePositiveInt(
                DEFAULT_AGENT_TIMEOUT_KEY,
                settingsProperties.getRuntime().getDefaultAgentTimeoutSeconds()
        );
    }

    /**
     * 返回默认最大步数。
     *
     * @return 默认最大步数
     */
    public int resolveDefaultMaxSteps() {
        return resolvePositiveInt(
                DEFAULT_MAX_STEPS_KEY,
                settingsProperties.getRuntime().getDefaultMaxSteps()
        );
    }

    /**
     * 返回默认最大 Agent 调用深度。
     *
     * @return 默认最大 Agent 调用深度
     */
    public int resolveDefaultMaxAgentCallDepth() {
        return resolvePositiveInt(
                DEFAULT_MAX_AGENT_CALL_DEPTH_KEY,
                settingsProperties.getRuntime().getDefaultMaxAgentCallDepth()
        );
    }

    /**
     * 读取正整数设置，查不到时回退启动配置。
     *
     * @param settingKey 设置键
     * @param fallbackValue 启动配置回退值
     * @return 最终值
     */
    private int resolvePositiveInt(String settingKey, int fallbackValue) {
        Map<String, SystemSettingRecord> records = systemSettingRepository.findByKeys(java.util.List.of(settingKey));
        SystemSettingRecord record = records.get(settingKey);
        if (record == null || record.settingValue() == null || record.settingValue().isBlank()) {
            return fallbackValue;
        }
        return Integer.parseInt(record.settingValue().trim());
    }
}
