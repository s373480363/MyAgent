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
     * Agent 默认总超时设置键。
     */
    public static final String DEFAULT_AGENT_TIMEOUT_KEY = "agent.studio.runtime.default-agent-timeout-seconds";

    /**
     * LLM 默认节点超时设置键。
     */
    public static final String DEFAULT_LLM_TIMEOUT_KEY = "agent.studio.runtime.default-llm-timeout-seconds";

    /**
     * Java 方法默认节点超时设置键。
     */
    public static final String DEFAULT_JAVA_METHOD_TIMEOUT_KEY = "agent.studio.runtime.default-java-method-timeout-seconds";

    /**
     * 外部 Agent 默认节点超时设置键。
     */
    public static final String DEFAULT_EXTERNAL_AGENT_TIMEOUT_KEY = "agent.studio.runtime.default-external-agent-timeout-seconds";

    /**
     * 默认最大步数设置键。
     */
    public static final String DEFAULT_MAX_STEPS_KEY = "agent.studio.runtime.default-max-steps";

    /**
     * 默认最大 Agent 调用深度设置键。
     */
    public static final String DEFAULT_MAX_AGENT_CALL_DEPTH_KEY = "agent.studio.runtime.default-max-agent-call-depth";

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
     * 返回 LLM 默认节点超时。
     *
     * @return LLM 默认节点超时
     */
    public int resolveDefaultLlmTimeoutSeconds() {
        return resolvePositiveInt(
                DEFAULT_LLM_TIMEOUT_KEY,
                settingsProperties.getRuntime().getDefaultLlmTimeoutSeconds()
        );
    }

    /**
     * 返回 Java 方法默认节点超时。
     *
     * @return Java 方法默认节点超时
     */
    public int resolveDefaultJavaMethodTimeoutSeconds() {
        return resolvePositiveInt(
                DEFAULT_JAVA_METHOD_TIMEOUT_KEY,
                settingsProperties.getRuntime().getDefaultJavaMethodTimeoutSeconds()
        );
    }

    /**
     * 返回外部 Agent 默认节点超时。
     *
     * @return 外部 Agent 默认节点超时
     */
    public int resolveDefaultExternalAgentTimeoutSeconds() {
        return resolvePositiveInt(
                DEFAULT_EXTERNAL_AGENT_TIMEOUT_KEY,
                settingsProperties.getRuntime().getDefaultExternalAgentTimeoutSeconds()
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
