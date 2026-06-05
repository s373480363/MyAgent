package com.myagent.model;

import com.myagent.common.error.BizException;
import com.myagent.common.error.ErrorCode;
import com.myagent.config.MyAgentSettingsProperties;
import com.myagent.settings.domain.PlatformSettingsResolver;
import com.myagent.settings.repository.SystemSettingRecord;
import com.myagent.settings.repository.SystemSettingRepository;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 模型调用统一超时执行器测试。
 */
class ModelInvocationTimeoutRunnerTests {

    /**
     * 超时后必须主动结束并返回统一业务错误。
     */
    @Test
    void runWithDefaultLlmTimeoutStopsLongRunningCall() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            ModelInvocationTimeoutRunner runner = new ModelInvocationTimeoutRunner(
                    platformSettingsResolver(1),
                    executorService
            );

            long startedAt = System.nanoTime();
            assertThatThrownBy(() -> runner.runWithDefaultLlmTimeout(() -> {
                Thread.sleep(5_000);
                return "unexpected";
            }, "模型供应商连接测试超时，请稍后重试或检查模型供应商网络连接。"))
                    .isInstanceOf(BizException.class)
                    .satisfies(exception -> {
                        BizException bizException = (BizException) exception;
                        assertThat(bizException.getErrorCode()).isEqualTo(ErrorCode.RUN_TIMEOUT);
                        assertThat(bizException.getMessage()).isEqualTo("模型供应商连接测试超时，请稍后重试或检查模型供应商网络连接。");
                    });
            long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;

            assertThat(elapsedMillis).isLessThan(3_000);
        } finally {
            executorService.shutdownNow();
        }
    }

    /**
     * 下层业务异常必须原样透传，不能被重包成第二套错误真相。
     */
    @Test
    void runWithDefaultLlmTimeoutKeepsBizException() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            ModelInvocationTimeoutRunner runner = new ModelInvocationTimeoutRunner(
                    platformSettingsResolver(3),
                    executorService
            );

            assertThatThrownBy(() -> runner.runWithDefaultLlmTimeout(
                    () -> {
                        throw new BizException(ErrorCode.NODE_EXECUTION_FAILED, "模型调用失败，请检查模型供应商配置、模型名称或网络连接。");
                    },
                    "模型供应商连接测试超时，请稍后重试或检查模型供应商网络连接。"
            ))
                    .isInstanceOf(BizException.class)
                    .satisfies(exception -> {
                        BizException bizException = (BizException) exception;
                        assertThat(bizException.getErrorCode()).isEqualTo(ErrorCode.NODE_EXECUTION_FAILED);
                        assertThat(bizException.getMessage()).isEqualTo("模型调用失败，请检查模型供应商配置、模型名称或网络连接。");
                    });
        } finally {
            executorService.shutdownNow();
        }
    }

    /**
     * 正常返回时应直接给出结果。
     */
    @Test
    void runWithDefaultLlmTimeoutReturnsSuccessfulResult() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            ModelInvocationTimeoutRunner runner = new ModelInvocationTimeoutRunner(
                    platformSettingsResolver(3),
                    executorService
            );

            String result = runner.runWithDefaultLlmTimeout(() -> "ok", "模型供应商连接测试超时，请稍后重试或检查模型供应商网络连接。");

            assertThat(result).isEqualTo("ok");
        } finally {
            executorService.shutdownNow();
        }
    }

    /**
     * 构造平台设置读取器。
     *
     * @param defaultLlmTimeoutSeconds 默认 LLM 超时秒数
     * @return 平台设置读取器
     */
    private PlatformSettingsResolver platformSettingsResolver(int defaultLlmTimeoutSeconds) {
        MyAgentSettingsProperties properties = new MyAgentSettingsProperties();
        properties.getRuntime().setDefaultLlmTimeoutSeconds(defaultLlmTimeoutSeconds);
        return new PlatformSettingsResolver(new InMemorySystemSettingRepository(), properties);
    }

    /**
     * 内存系统设置仓储。
     */
    private static final class InMemorySystemSettingRepository implements SystemSettingRepository {

        /**
         * 系统设置覆盖值。
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
            records.put(record.settingKey(), record);
        }
    }
}
