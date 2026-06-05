package com.myagent.settings.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myagent.common.api.ApiError;
import com.myagent.common.error.BizException;
import com.myagent.common.error.ErrorCode;
import com.myagent.config.MyAgentSettingsProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 系统设置白名单目录。
 */
@Component
public class SettingsCatalog {

    /**
     * 白名单项映射。
     */
    private final Map<String, SettingCatalogItem> items;

    /**
     * JSON 对象映射器。
     */
    private final ObjectMapper objectMapper;

    /**
     * 构造系统设置白名单目录。
     *
     * @param objectMapper JSON 对象映射器
     */
    public SettingsCatalog(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        Map<String, SettingCatalogItem> catalog = new LinkedHashMap<>();
        register(catalog, new SettingCatalogItem(
                "agent.studio.runtime.default-agent-timeout-seconds",
                SettingValueType.NUMBER,
                true,
                "Agent 默认总超时（秒）",
                properties -> String.valueOf(properties.getRuntime().getDefaultAgentTimeoutSeconds())
        ));
        register(catalog, new SettingCatalogItem(
                "agent.studio.runtime.default-llm-timeout-seconds",
                SettingValueType.NUMBER,
                true,
                "LLM 节点默认超时（秒）",
                properties -> String.valueOf(properties.getRuntime().getDefaultLlmTimeoutSeconds())
        ));
        register(catalog, new SettingCatalogItem(
                "agent.studio.runtime.default-java-method-timeout-seconds",
                SettingValueType.NUMBER,
                true,
                "Java 方法节点默认超时（秒）",
                properties -> String.valueOf(properties.getRuntime().getDefaultJavaMethodTimeoutSeconds())
        ));
        register(catalog, new SettingCatalogItem(
                "agent.studio.runtime.default-external-agent-timeout-seconds",
                SettingValueType.NUMBER,
                true,
                "外部 Agent 节点默认超时（秒）",
                properties -> String.valueOf(properties.getRuntime().getDefaultExternalAgentTimeoutSeconds())
        ));
        register(catalog, new SettingCatalogItem(
                "agent.studio.runtime.default-max-steps",
                SettingValueType.NUMBER,
                true,
                "默认最大步数",
                properties -> String.valueOf(properties.getRuntime().getDefaultMaxSteps())
        ));
        register(catalog, new SettingCatalogItem(
                "agent.studio.runtime.default-max-agent-call-depth",
                SettingValueType.NUMBER,
                true,
                "默认最大 Agent 调用深度",
                properties -> String.valueOf(properties.getRuntime().getDefaultMaxAgentCallDepth())
        ));
        this.items = Collections.unmodifiableMap(new LinkedHashMap<>(catalog));
    }

    /**
     * 返回全部白名单项，保持定义顺序。
     *
     * @return 白名单项列表
     */
    public List<SettingCatalogItem> listItems() {
        return List.copyOf(items.values());
    }

    /**
     * 按键查找白名单项。
     *
     * @param settingKey 设置键
     * @return 白名单项
     */
    public Optional<SettingCatalogItem> find(String settingKey) {
        return Optional.ofNullable(items.get(settingKey));
    }

    /**
     * 返回指定设置键对应的白名单项。
     *
     * @param settingKey 设置键
     * @return 白名单项
     */
    public SettingCatalogItem getRequired(String settingKey) {
        SettingCatalogItem item = items.get(settingKey);
        if (item == null) {
            throw new BizException(
                    ErrorCode.INVALID_ARGUMENT,
                    "设置键不在 V1 白名单范围内，不允许通过 /api/settings 访问。",
                    List.of(new ApiError.Detail("$.items[*].settingKey", "not_allowed", "设置键不在白名单内。", null, settingKey, null))
            );
        }
        return item;
    }

    /**
     * 校验设置值是否符合目录定义。
     *
     * @param item 白名单项
     * @param valueType 值类型
     * @param settingValue 设置值
     */
    public void validate(SettingCatalogItem item, SettingValueType valueType, String settingValue) {
        if (valueType != item.getValueType()) {
            throw new BizException(
                    ErrorCode.INVALID_ARGUMENT,
                    "设置值类型与白名单定义不一致。"
            );
        }
        if (!item.isEditable()) {
            throw new BizException(ErrorCode.SETTING_NOT_EDITABLE, "该系统设置不可编辑。");
        }
        if (settingValue == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "设置值不能为空。");
        }
        switch (item.getValueType()) {
            case NUMBER -> validateNumber(item, settingValue);
            case BOOLEAN -> validateBoolean(settingValue);
            case JSON -> validateJson(settingValue);
            case STRING -> {
                throw new BizException(ErrorCode.INVALID_ARGUMENT, "当前白名单不支持字符串类型设置。");
            }
            default -> throw new BizException(ErrorCode.INVALID_ARGUMENT, "未知的设置值类型。");
        }
    }

    /**
     * 注册白名单项。
     *
     * @param catalog 目录映射
     * @param item 白名单项
     */
    private void register(Map<String, SettingCatalogItem> catalog, SettingCatalogItem item) {
        catalog.put(item.getSettingKey(), item);
    }

    /**
     * 校验数字值。
     *
     * @param item 白名单项
     * @param settingValue 设置值
     */
    private void validateNumber(SettingCatalogItem item, String settingValue) {
        try {
            BigDecimal number = new BigDecimal(settingValue.trim());
            if (number.scale() > 0 || number.compareTo(BigDecimal.ONE) < 0) {
                throw new BizException(ErrorCode.INVALID_ARGUMENT, item.getDescription() + "必须是大于等于 1 的整数。");
            }
        } catch (NumberFormatException exception) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, item.getDescription() + "必须是数字。");
        }
    }

    /**
     * 校验布尔值。
     *
     * @param settingValue 设置值
     */
    private void validateBoolean(String settingValue) {
        String normalized = settingValue.trim().toLowerCase(Locale.ROOT);
        if (!"true".equals(normalized) && !"false".equals(normalized)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "布尔类型设置值必须为 true 或 false。");
        }
    }

    /**
     * 校验 JSON 值。
     *
     * @param settingValue 设置值
     */
    private void validateJson(String settingValue) {
        try {
            objectMapper.readTree(settingValue);
        } catch (Exception exception) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "JSON 类型设置值格式不正确。");
        }
    }
}
