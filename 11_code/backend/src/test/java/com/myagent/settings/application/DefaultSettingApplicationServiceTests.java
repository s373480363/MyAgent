package com.myagent.settings.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myagent.common.error.BizException;
import com.myagent.config.MyAgentSettingsProperties;
import com.myagent.settings.application.command.UpdateSettingsCommand;
import com.myagent.settings.domain.SettingSource;
import com.myagent.settings.domain.SettingValueType;
import com.myagent.settings.domain.SettingsCatalog;
import com.myagent.settings.repository.SystemSettingRecord;
import com.myagent.settings.repository.SystemSettingRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 系统设置应用服务测试。
 */
class DefaultSettingApplicationServiceTests {

    /**
     * JSON 对象映射器。
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 查询设置时应按白名单返回当前生效值，并区分数据库覆盖值和启动配置值来源。
     */
    @Test
    void getSettingsReturnsWhitelistWithCorrectSource() {
        InMemorySystemSettingRepository repository = new InMemorySystemSettingRepository();
        repository.upsert(new SystemSettingRecord(
                "myagent.openai.default-model",
                "gpt-5-mini",
                SettingValueType.STRING,
                "默认模型",
                true,
                Instant.now()
        ));
        DefaultSettingApplicationService service = newService(repository);

        var settings = service.getSettings();

        assertThat(settings).hasSize(7);
        assertThat(settings.getFirst().settingKey()).isEqualTo("myagent.openai.default-model");
        assertThat(settings.getFirst().settingValue()).isEqualTo("gpt-5-mini");
        assertThat(settings.getFirst().source()).isEqualTo(SettingSource.SYSTEM_SETTING);
        assertThat(settings.stream()
                .filter(item -> item.settingKey().equals("myagent.runtime.default-agent-timeout-seconds"))
                .findFirst()
                .orElseThrow()
                .source()).isEqualTo(SettingSource.APPLICATION_CONFIG);
    }

    /**
     * 更新白名单设置后，应写入覆盖值并使用白名单定义的值类型。
     */
    @Test
    void updateSettingsPersistsOverrideValue() {
        InMemorySystemSettingRepository repository = new InMemorySystemSettingRepository();
        DefaultSettingApplicationService service = newService(repository);

        service.updateSettings(new UpdateSettingsCommand(List.of(
                new UpdateSettingsCommand.Item(
                        "myagent.runtime.default-max-steps",
                        "60",
                        SettingValueType.NUMBER
                )
        )));

        SystemSettingRecord record = repository.records.get("myagent.runtime.default-max-steps");
        assertThat(record).isNotNull();
        assertThat(record.settingValue()).isEqualTo("60");
        assertThat(record.valueType()).isEqualTo(SettingValueType.NUMBER);
    }

    /**
     * 旧超时键必须被直接拒绝，不能继续作为兼容入口。
     */
    @Test
    void updateSettingsRejectsDeprecatedTimeoutKey() {
        DefaultSettingApplicationService service = newService(new InMemorySystemSettingRepository());

        assertThatThrownBy(() -> service.updateSettings(new UpdateSettingsCommand(List.of(
                new UpdateSettingsCommand.Item(
                        "myagent.runtime.default-timeout-seconds",
                        "120",
                        SettingValueType.NUMBER
                )
        )))).isInstanceOf(BizException.class)
                .hasMessageContaining("已废弃");
    }

    /**
     * 非白名单键必须被拒绝。
     */
    @Test
    void updateSettingsRejectsIllegalKey() {
        DefaultSettingApplicationService service = newService(new InMemorySystemSettingRepository());

        assertThatThrownBy(() -> service.updateSettings(new UpdateSettingsCommand(List.of(
                new UpdateSettingsCommand.Item(
                        "myagent.openai.api-key",
                        "secret",
                        SettingValueType.STRING
                )
        )))).isInstanceOf(BizException.class)
                .hasMessageContaining("白名单");
    }

    /**
     * 值类型与白名单定义不一致时必须拒绝更新。
     */
    @Test
    void updateSettingsRejectsTypeMismatch() {
        DefaultSettingApplicationService service = newService(new InMemorySystemSettingRepository());

        assertThatThrownBy(() -> service.updateSettings(new UpdateSettingsCommand(List.of(
                new UpdateSettingsCommand.Item(
                        "myagent.openai.default-model",
                        "123",
                        SettingValueType.NUMBER
                )
        )))).isInstanceOf(BizException.class)
                .hasMessageContaining("值类型");
    }

    /**
     * 创建应用服务。
     *
     * @param repository 内存仓储
     * @return 应用服务
     */
    private DefaultSettingApplicationService newService(InMemorySystemSettingRepository repository) {
        MyAgentSettingsProperties properties = new MyAgentSettingsProperties();
        properties.getOpenai().setDefaultModel("gpt-4.1-mini");
        properties.getRuntime().setDefaultAgentTimeoutSeconds(600);
        properties.getRuntime().setDefaultLlmTimeoutSeconds(120);
        properties.getRuntime().setDefaultJavaMethodTimeoutSeconds(30);
        properties.getRuntime().setDefaultExternalAgentTimeoutSeconds(600);
        properties.getRuntime().setDefaultMaxSteps(30);
        properties.getRuntime().setDefaultMaxAgentCallDepth(3);
        return new DefaultSettingApplicationService(
                repository,
                new SettingsCatalog(OBJECT_MAPPER),
                properties
        );
    }

    /**
     * 内存系统设置仓储。
     */
    private static final class InMemorySystemSettingRepository implements SystemSettingRepository {

        /**
         * 数据记录。
         */
        private final Map<String, SystemSettingRecord> records = new LinkedHashMap<>();

        @Override
        public Map<String, SystemSettingRecord> findByKeys(List<String> settingKeys) {
            Map<String, SystemSettingRecord> result = new LinkedHashMap<>();
            for (String settingKey : settingKeys) {
                if (records.containsKey(settingKey)) {
                    result.put(settingKey, records.get(settingKey));
                }
            }
            return result;
        }

        @Override
        public void upsert(SystemSettingRecord record) {
            records.put(record.settingKey(), new SystemSettingRecord(
                    record.settingKey(),
                    record.settingValue(),
                    record.valueType(),
                    record.description(),
                    record.editable(),
                    Instant.now()
            ));
        }
    }
}
