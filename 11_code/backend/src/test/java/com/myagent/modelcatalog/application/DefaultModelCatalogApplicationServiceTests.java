package com.myagent.modelcatalog.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myagent.common.domain.EnableStatus;
import com.myagent.common.error.BizException;
import com.myagent.common.error.ErrorCode;
import com.myagent.common.page.PageResult;
import com.myagent.config.MyAgentSettingsProperties;
import com.myagent.model.ModelInvocationResult;
import com.myagent.model.ModelInvocationTimeoutRunner;
import com.myagent.model.OpenAiCompatibleModelInvoker;
import com.myagent.model.ResolvedModelRoute;
import com.myagent.modelcatalog.application.command.TestModelProviderCommand;
import com.myagent.modelcatalog.application.query.ListModelOfferingsQuery;
import com.myagent.modelcatalog.application.query.ListModelProvidersQuery;
import com.myagent.modelcatalog.application.result.ModelOfferingDescriptor;
import com.myagent.modelcatalog.domain.ModelProviderType;
import com.myagent.modelcatalog.repository.ModelOfferingJoinedRecord;
import com.myagent.modelcatalog.repository.ModelOfferingRecord;
import com.myagent.modelcatalog.repository.ModelOfferingRepository;
import com.myagent.modelcatalog.repository.ModelProviderRecord;
import com.myagent.modelcatalog.repository.ModelProviderRepository;
import com.myagent.modelcatalog.secret.ModelProviderSecretService;
import com.myagent.settings.domain.PlatformSettingsResolver;
import com.myagent.settings.repository.SystemSettingRecord;
import com.myagent.settings.repository.SystemSettingRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 模型目录应用服务测试。
 */
class DefaultModelCatalogApplicationServiceTests {

    /**
     * 连接测试成功时应返回供应商与供应项摘要。
     */
    @Test
    void testModelProviderReturnsSuccessSummary() {
        Fixture fixture = new Fixture(new SuccessInvoker());
        try {
            var result = fixture.service.testModelProvider(new TestModelProviderCommand(1L, "openai.gpt_4_1_mini", "ping"));

            assertThat(result.providerKey()).isEqualTo("openai");
            assertThat(result.offeringKey()).isEqualTo("openai.gpt_4_1_mini");
            assertThat(result.modelKey()).isEqualTo("gpt_4_1_mini");
            assertThat(result.upstreamModelName()).isEqualTo("gpt-4.1-mini");
            assertThat(result.durationMs()).isEqualTo(123L);
            assertThat(result.message()).isEqualTo("模型供应商连接测试成功。");
        } finally {
            fixture.close();
        }
    }

    /**
     * 连接测试超时后必须返回统一中文业务错误，不允许长期挂起。
     */
    @Test
    void testModelProviderStopsWhenInvocationTimeout() {
        Fixture fixture = new Fixture(new HangingInvoker());
        try {
            long startedAt = System.nanoTime();
            assertThatThrownBy(() -> fixture.service.testModelProvider(new TestModelProviderCommand(1L, "openai.gpt_4_1_mini", "ping")))
                    .isInstanceOf(BizException.class)
                    .satisfies(exception -> {
                        BizException bizException = (BizException) exception;
                        assertThat(bizException.getErrorCode()).isEqualTo(ErrorCode.RUN_TIMEOUT);
                        assertThat(bizException.getMessage()).isEqualTo("模型供应商连接测试超时，请稍后重试或检查模型供应商网络连接。");
                    });
            long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;

            assertThat(elapsedMillis).isLessThan(3_000);
        } finally {
            fixture.close();
        }
    }

    /**
     * 连接测试失败时只能返回脱敏中文摘要，不能把上游敏感细节透出。
     */
    @Test
    void testModelProviderKeepsFailureMessageSanitized() {
        Fixture fixture = new Fixture(new SensitiveFailureInvoker());
        try {
            assertThatThrownBy(() -> fixture.service.testModelProvider(new TestModelProviderCommand(1L, "openai.gpt_4_1_mini", "ping")))
                    .isInstanceOf(BizException.class)
                    .satisfies(exception -> {
                        BizException bizException = (BizException) exception;
                        assertThat(bizException.getErrorCode()).isEqualTo(ErrorCode.NODE_EXECUTION_FAILED);
                        assertThat(bizException.getMessage()).isEqualTo("模型调用失败，请检查模型供应商配置、模型名称或网络连接。");
                        assertThat(bizException.getMessage()).doesNotContain("sk-secret");
                        assertThat(bizException.getMessage()).doesNotContain("https://internal.example.com");
                    });
        } finally {
            fixture.close();
        }
    }

    /**
     * 测试夹具。
     */
    private static final class Fixture implements AutoCloseable {

        /**
         * 执行线程池。
         */
        private final ExecutorService executorService = Executors.newSingleThreadExecutor();

        /**
         * 应用服务。
         */
        private final DefaultModelCatalogApplicationService service;

        /**
         * 构造夹具。
         *
         * @param invoker 调用器
         */
        private Fixture(OpenAiCompatibleModelInvoker invoker) {
            InMemoryModelProviderRepository modelProviderRepository = new InMemoryModelProviderRepository();
            InMemoryModelOfferingRepository modelOfferingRepository = new InMemoryModelOfferingRepository();
            StubModelProviderSecretService secretService = new StubModelProviderSecretService();
            ModelRouteResolver modelRouteResolver = new ModelRouteResolver(
                    modelOfferingRepository,
                    modelProviderRepository,
                    secretService
            );
            this.service = new DefaultModelCatalogApplicationService(
                    modelProviderRepository,
                    modelOfferingRepository,
                    new NoopModelOfferingRegistry(),
                    modelRouteResolver,
                    secretService,
                    invoker,
                    new ModelInvocationTimeoutRunner(platformSettingsResolver(), executorService)
            );
        }

        @Override
        public void close() {
            executorService.shutdownNow();
        }

        /**
         * 构造平台设置读取器。
         *
         * @return 平台设置读取器
         */
        private PlatformSettingsResolver platformSettingsResolver() {
            MyAgentSettingsProperties properties = new MyAgentSettingsProperties();
            properties.getRuntime().setDefaultLlmTimeoutSeconds(1);
            return new PlatformSettingsResolver(new InMemorySystemSettingRepository(), properties);
        }
    }

    /**
     * 成功调用器。
     */
    private static final class SuccessInvoker extends OpenAiCompatibleModelInvoker {

        /**
         * 构造成功调用器。
         */
        private SuccessInvoker() {
            super(new ObjectMapper());
        }

        @Override
        public ModelInvocationResult invoke(ResolvedModelRoute route, com.myagent.model.ModelInvocationRequest request) {
            return new ModelInvocationResult(
                    new ObjectMapper().getNodeFactory().textNode("pong"),
                    "pong",
                    123L
            );
        }
    }

    /**
     * 长时间挂起调用器。
     */
    private static final class HangingInvoker extends OpenAiCompatibleModelInvoker {

        /**
         * 构造挂起调用器。
         */
        private HangingInvoker() {
            super(new ObjectMapper());
        }

        @Override
        public ModelInvocationResult invoke(ResolvedModelRoute route, com.myagent.model.ModelInvocationRequest request) {
            try {
                Thread.sleep(5_000);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new BizException(ErrorCode.RUN_TIMEOUT, "模型调用被中断。");
            }
            throw new IllegalStateException("should not reach");
        }
    }

    /**
     * 带敏感信息的失败调用器。
     */
    private static final class SensitiveFailureInvoker extends OpenAiCompatibleModelInvoker {

        /**
         * 构造失败调用器。
         */
        private SensitiveFailureInvoker() {
            super(new ObjectMapper());
        }

        @Override
        public ModelInvocationResult invoke(ResolvedModelRoute route, com.myagent.model.ModelInvocationRequest request) {
            throw new RuntimeException("401 Unauthorized: apiKey=sk-secret, baseUrl=https://internal.example.com/v1");
        }
    }

    /**
     * 空实现供应项注册表。
     */
    private static final class NoopModelOfferingRegistry implements ModelOfferingRegistry {

        @Override
        public PageResult<ModelOfferingDescriptor> listEnabled(ListModelOfferingsQuery query) {
            return PageResult.empty(query.page(), query.pageSize());
        }

        @Override
        public Optional<ModelOfferingDescriptor> findByOfferingKey(String offeringKey) {
            return Optional.empty();
        }

        @Override
        public List<ModelOfferingDescriptor> findByOfferingKeys(List<String> offeringKeys) {
            return List.of();
        }
    }

    /**
     * 内存模型供应商仓储。
     */
    private static final class InMemoryModelProviderRepository implements ModelProviderRepository {

        /**
         * 供应商数据。
         */
        private final Map<Long, ModelProviderRecord> records = new LinkedHashMap<>();

        /**
         * 构造仓储。
         */
        private InMemoryModelProviderRepository() {
            Instant now = Instant.parse("2026-06-05T00:00:00Z");
            records.put(1L, new ModelProviderRecord(
                    1L,
                    "openai",
                    "OpenAI",
                    ModelProviderType.OPENAI_COMPATIBLE,
                    "https://api.openai.com/v1",
                    "ciphertext",
                    "sk-...abcd",
                    EnableStatus.ENABLED,
                    "provider",
                    now,
                    now
            ));
        }

        @Override
        public PageResult<ModelProviderRecord> listProviders(ListModelProvidersQuery query) {
            return PageResult.of(records.values().stream().toList(), query.page(), query.pageSize(), records.size());
        }

        @Override
        public Optional<ModelProviderRecord> findById(long providerId) {
            return Optional.ofNullable(records.get(providerId));
        }

        @Override
        public Optional<ModelProviderRecord> findByProviderKey(String providerKey) {
            return records.values().stream().filter(record -> record.providerKey().equals(providerKey)).findFirst();
        }

        @Override
        public ModelProviderRecord insert(ModelProviderRecord record) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int update(ModelProviderRecord record) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int updateStatus(long providerId, EnableStatus status) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * 内存模型供应项仓储。
     */
    private static final class InMemoryModelOfferingRepository implements ModelOfferingRepository {

        /**
         * 供应项数据。
         */
        private final Map<Long, ModelOfferingRecord> records = new LinkedHashMap<>();

        /**
         * 构造仓储。
         */
        private InMemoryModelOfferingRepository() {
            Instant now = Instant.parse("2026-06-05T00:00:00Z");
            records.put(10L, new ModelOfferingRecord(
                    10L,
                    "openai.gpt_4_1_mini",
                    "openai",
                    "gpt_4_1_mini",
                    "GPT-4.1 mini",
                    "gpt-4.1-mini",
                    BigDecimal.ZERO,
                    EnableStatus.ENABLED,
                    "offering",
                    now,
                    now
            ));
        }

        @Override
        public PageResult<ModelOfferingJoinedRecord> listOfferings(ListModelOfferingsQuery query) {
            return PageResult.empty(query.page(), query.pageSize());
        }

        @Override
        public Optional<ModelOfferingRecord> findById(long offeringId) {
            return Optional.ofNullable(records.get(offeringId));
        }

        @Override
        public Optional<ModelOfferingRecord> findByOfferingKey(String offeringKey) {
            return records.values().stream().filter(record -> record.offeringKey().equals(offeringKey)).findFirst();
        }

        @Override
        public Optional<ModelOfferingJoinedRecord> findJoinedByOfferingKey(String offeringKey) {
            return Optional.empty();
        }

        @Override
        public List<ModelOfferingJoinedRecord> findJoinedByOfferingKeys(List<String> offeringKeys) {
            return List.of();
        }

        @Override
        public Optional<ModelOfferingRecord> findByProviderKeyAndUpstreamModelName(String providerKey, String upstreamModelName) {
            return Optional.empty();
        }

        @Override
        public ModelOfferingRecord insert(ModelOfferingRecord record) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int update(ModelOfferingRecord record) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int updateStatus(long offeringId, EnableStatus status) {
            throw new UnsupportedOperationException();
        }
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

    /**
     * 桩密钥服务。
     */
    private static final class StubModelProviderSecretService implements ModelProviderSecretService {

        @Override
        public String encrypt(String apiKey) {
            return "cipher-" + apiKey;
        }

        @Override
        public String decrypt(String ciphertext) {
            return "sk-secret";
        }

        @Override
        public String mask(String apiKey) {
            return "sk-...abcd";
        }

        @Override
        public boolean isConfigured(String ciphertext) {
            return ciphertext != null && !ciphertext.isBlank();
        }
    }
}
