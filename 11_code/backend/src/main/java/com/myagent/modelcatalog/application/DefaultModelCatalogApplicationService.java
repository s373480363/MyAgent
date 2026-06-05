package com.myagent.modelcatalog.application;

import com.myagent.common.domain.EnableStatus;
import com.myagent.common.error.BizException;
import com.myagent.common.error.ErrorCode;
import com.myagent.common.page.PageResult;
import com.myagent.model.ModelInvocationRequest;
import com.myagent.model.ModelInvocationTimeoutRunner;
import com.myagent.model.OpenAiCompatibleModelInvoker;
import com.myagent.modelcatalog.application.command.ChangeModelOfferingStatusCommand;
import com.myagent.modelcatalog.application.command.ChangeModelProviderStatusCommand;
import com.myagent.modelcatalog.application.command.CreateModelOfferingCommand;
import com.myagent.modelcatalog.application.command.CreateModelProviderCommand;
import com.myagent.modelcatalog.application.command.TestModelProviderCommand;
import com.myagent.modelcatalog.application.command.UpdateModelOfferingCommand;
import com.myagent.modelcatalog.application.command.UpdateModelProviderCommand;
import com.myagent.modelcatalog.application.command.UpdateModelProviderSecretsCommand;
import com.myagent.modelcatalog.application.query.GetModelProviderQuery;
import com.myagent.modelcatalog.application.query.ListModelOfferingsQuery;
import com.myagent.modelcatalog.application.query.ListModelProvidersQuery;
import com.myagent.modelcatalog.application.result.ModelOfferingBatchResult;
import com.myagent.modelcatalog.application.result.ModelOfferingDescriptor;
import com.myagent.modelcatalog.application.result.ModelProviderResult;
import com.myagent.modelcatalog.application.result.ModelProviderTestResult;
import com.myagent.modelcatalog.domain.ModelProviderType;
import com.myagent.modelcatalog.repository.ModelOfferingRecord;
import com.myagent.modelcatalog.repository.ModelOfferingRepository;
import com.myagent.modelcatalog.repository.ModelProviderRecord;
import com.myagent.modelcatalog.repository.ModelProviderRepository;
import com.myagent.modelcatalog.secret.ModelProviderSecretService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 默认模型目录应用服务。
 */
@Service
public class DefaultModelCatalogApplicationService implements ModelCatalogApplicationService {

    /**
     * 模型供应商仓储。
     */
    private final ModelProviderRepository modelProviderRepository;

    /**
     * 模型供应项仓储。
     */
    private final ModelOfferingRepository modelOfferingRepository;

    /**
     * 模型供应项注册表。
     */
    private final ModelOfferingRegistry modelOfferingRegistry;

    /**
     * 路由解析器。
     */
    private final ModelRouteResolver modelRouteResolver;

    /**
     * 密钥服务。
     */
    private final ModelProviderSecretService modelProviderSecretService;

    /**
     * OpenAI-compatible 调用器。
     */
    private final OpenAiCompatibleModelInvoker openAiCompatibleModelInvoker;

    /**
     * 模型调用统一超时执行器。
     */
    private final ModelInvocationTimeoutRunner modelInvocationTimeoutRunner;

    /**
     * 构造应用服务。
     *
     * @param modelProviderRepository 模型供应商仓储
     * @param modelOfferingRepository 模型供应项仓储
     * @param modelOfferingRegistry 模型供应项注册表
     * @param modelRouteResolver 路由解析器
     * @param modelProviderSecretService 密钥服务
     * @param openAiCompatibleModelInvoker OpenAI-compatible 调用器
     * @param modelInvocationTimeoutRunner 模型调用统一超时执行器
     */
    public DefaultModelCatalogApplicationService(
            ModelProviderRepository modelProviderRepository,
            ModelOfferingRepository modelOfferingRepository,
            ModelOfferingRegistry modelOfferingRegistry,
            ModelRouteResolver modelRouteResolver,
            ModelProviderSecretService modelProviderSecretService,
            OpenAiCompatibleModelInvoker openAiCompatibleModelInvoker,
            ModelInvocationTimeoutRunner modelInvocationTimeoutRunner
    ) {
        this.modelProviderRepository = modelProviderRepository;
        this.modelOfferingRepository = modelOfferingRepository;
        this.modelOfferingRegistry = modelOfferingRegistry;
        this.modelRouteResolver = modelRouteResolver;
        this.modelProviderSecretService = modelProviderSecretService;
        this.openAiCompatibleModelInvoker = openAiCompatibleModelInvoker;
        this.modelInvocationTimeoutRunner = modelInvocationTimeoutRunner;
    }

    @Override
    public PageResult<ModelProviderResult> listModelProviders(ListModelProvidersQuery query) {
        return modelProviderRepository.listProviders(query).map(this::toProviderResult);
    }

    @Override
    public ModelProviderResult getModelProvider(GetModelProviderQuery query) {
        return toProviderResult(requiredProvider(query.providerId()));
    }

    @Override
    @Transactional
    public ModelProviderResult createModelProvider(CreateModelProviderCommand command) {
        validateProviderType(command.providerType());
        if (modelProviderRepository.findByProviderKey(requiredText(command.providerKey(), "providerKey 不能为空。")).isPresent()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "providerKey 已存在，不能重复创建。");
        }
        ModelProviderRecord record = modelProviderRepository.insert(new ModelProviderRecord(
                0L,
                command.providerKey().trim(),
                requiredText(command.name(), "名称不能为空。"),
                command.providerType(),
                requiredText(command.baseUrl(), "Base URL 不能为空。"),
                encrypt(command.apiKey()),
                mask(command.apiKey()),
                EnableStatus.ENABLED,
                normalizedText(command.description()),
                null,
                null
        ));
        return toProviderResult(record);
    }

    @Override
    @Transactional
    public ModelProviderResult updateModelProvider(UpdateModelProviderCommand command) {
        ModelProviderRecord existing = requiredProvider(command.providerId());
        modelProviderRepository.update(new ModelProviderRecord(
                existing.id(),
                existing.providerKey(),
                requiredText(command.name(), "名称不能为空。"),
                existing.providerType(),
                requiredText(command.baseUrl(), "Base URL 不能为空。"),
                existing.apiKeyCiphertext(),
                existing.apiKeyMask(),
                existing.status(),
                normalizedText(command.description()),
                existing.createdAt(),
                existing.updatedAt()
        ));
        return toProviderResult(requiredProvider(command.providerId()));
    }

    @Override
    @Transactional
    public ModelProviderResult updateModelProviderSecrets(UpdateModelProviderSecretsCommand command) {
        ModelProviderRecord existing = requiredProvider(command.providerId());
        String ciphertext = existing.apiKeyCiphertext();
        String apiKeyMask = existing.apiKeyMask();
        if (command.clearApiKey()) {
            ciphertext = "";
            apiKeyMask = "";
        } else if (command.apiKey() != null && !command.apiKey().isBlank()) {
            ciphertext = encrypt(command.apiKey());
            apiKeyMask = mask(command.apiKey());
        }
        modelProviderRepository.update(new ModelProviderRecord(
                existing.id(),
                existing.providerKey(),
                existing.name(),
                existing.providerType(),
                existing.baseUrl(),
                ciphertext,
                apiKeyMask,
                existing.status(),
                existing.description(),
                existing.createdAt(),
                existing.updatedAt()
        ));
        return toProviderResult(requiredProvider(command.providerId()));
    }

    @Override
    @Transactional
    public void changeModelProviderStatus(ChangeModelProviderStatusCommand command) {
        if (command.status() == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "status 不能为空。");
        }
        requiredProvider(command.providerId());
        modelProviderRepository.updateStatus(command.providerId(), command.status());
    }

    @Override
    public ModelProviderTestResult testModelProvider(TestModelProviderCommand command) {
        String offeringKey = requiredText(command.offeringKey(), "offeringKey 不能为空。");
        var route = modelRouteResolver.resolveForProviderTest(command.providerId(), offeringKey);
        var result = modelInvocationTimeoutRunner.runWithDefaultLlmTimeout(
                () -> openAiCompatibleModelInvoker.invoke(
                        route,
                        new ModelInvocationRequest(
                                offeringKey,
                                null,
                                command.prompt() == null || command.prompt().isBlank() ? "ping" : command.prompt().trim(),
                                null,
                                BigDecimal.ZERO,
                                false
                        )
                ),
                "模型供应商连接测试超时，请稍后重试或检查模型供应商网络连接。"
        );
        return new ModelProviderTestResult(
                route.providerKey(),
                route.offeringKey(),
                route.modelKey(),
                route.upstreamModelName(),
                result.durationMs(),
                "模型供应商连接测试成功。"
        );
    }

    @Override
    public PageResult<ModelOfferingDescriptor> listModelOfferings(ListModelOfferingsQuery query) {
        return modelOfferingRegistry.listEnabled(query);
    }

    @Override
    public ModelOfferingDescriptor getModelOffering(String offeringKey) {
        return modelOfferingRegistry.findByOfferingKey(offeringKey)
                .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND, "指定模型供应项不存在。"));
    }

    @Override
    public ModelOfferingBatchResult getModelOfferingsByKeys(List<String> offeringKeys) {
        List<String> normalizedKeys = offeringKeys == null ? List.of() : offeringKeys.stream()
                .filter(key -> key != null && !key.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
        List<ModelOfferingDescriptor> items = modelOfferingRegistry.findByOfferingKeys(normalizedKeys);
        Map<String, ModelOfferingDescriptor> itemsByKey = new LinkedHashMap<>();
        for (ModelOfferingDescriptor item : items) {
            itemsByKey.put(item.offeringKey(), item);
        }
        List<ModelOfferingDescriptor> orderedItems = new ArrayList<>();
        List<String> missingKeys = new ArrayList<>();
        for (String offeringKey : normalizedKeys) {
            ModelOfferingDescriptor item = itemsByKey.get(offeringKey);
            if (item == null) {
                missingKeys.add(offeringKey);
            } else {
                orderedItems.add(item);
            }
        }
        return new ModelOfferingBatchResult(orderedItems, missingKeys);
    }

    @Override
    @Transactional
    public ModelOfferingDescriptor createModelOffering(CreateModelOfferingCommand command) {
        String offeringKey = requiredText(command.offeringKey(), "offeringKey 不能为空。");
        if (modelOfferingRepository.findByOfferingKey(offeringKey).isPresent()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "offeringKey 已存在，不能重复创建。");
        }
        ModelProviderRecord provider = requiredProvider(requiredText(command.providerKey(), "providerKey 不能为空。"));
        ensureUniqueProviderUpstream(provider.providerKey(), command.upstreamModelName(), null);
        ModelOfferingRecord inserted = modelOfferingRepository.insert(new ModelOfferingRecord(
                0L,
                offeringKey.trim(),
                provider.providerKey(),
                requiredText(command.modelKey(), "modelKey 不能为空。"),
                requiredText(command.displayName(), "displayName 不能为空。"),
                requiredText(command.upstreamModelName(), "upstreamModelName 不能为空。"),
                normalizedTemperature(command.defaultTemperature()),
                EnableStatus.ENABLED,
                normalizedText(command.description()),
                null,
                null
        ));
        return getModelOffering(inserted.offeringKey());
    }

    @Override
    @Transactional
    public ModelOfferingDescriptor updateModelOffering(UpdateModelOfferingCommand command) {
        ModelOfferingRecord existing = requiredOffering(command.offeringId());
        ModelProviderRecord provider = requiredProvider(requiredText(command.providerKey(), "providerKey 不能为空。"));
        ensureUniqueProviderUpstream(provider.providerKey(), command.upstreamModelName(), existing.id());
        modelOfferingRepository.update(new ModelOfferingRecord(
                existing.id(),
                existing.offeringKey(),
                provider.providerKey(),
                requiredText(command.modelKey(), "modelKey 不能为空。"),
                requiredText(command.displayName(), "displayName 不能为空。"),
                requiredText(command.upstreamModelName(), "upstreamModelName 不能为空。"),
                normalizedTemperature(command.defaultTemperature()),
                existing.status(),
                normalizedText(command.description()),
                existing.createdAt(),
                existing.updatedAt()
        ));
        return getModelOffering(existing.offeringKey());
    }

    @Override
    @Transactional
    public ModelOfferingDescriptor changeModelOfferingStatus(ChangeModelOfferingStatusCommand command) {
        if (command.status() == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "status 不能为空。");
        }
        ModelOfferingRecord offering = requiredOffering(command.offeringId());
        modelOfferingRepository.updateStatus(command.offeringId(), command.status());
        return getModelOffering(offering.offeringKey());
    }

    /**
     * 校验供应商类型。
     *
     * @param providerType 供应商类型
     */
    private void validateProviderType(ModelProviderType providerType) {
        if (providerType != ModelProviderType.OPENAI_COMPATIBLE) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "当前版本只支持 OPENAI_COMPATIBLE 模型供应商。");
        }
    }

    /**
     * 确保供应商与上游模型组合唯一。
     *
     * @param providerKey 供应商标识
     * @param upstreamModelName 上游模型名
     * @param currentOfferingId 当前供应项主键
     */
    private void ensureUniqueProviderUpstream(String providerKey, String upstreamModelName, Long currentOfferingId) {
        String normalizedUpstreamModelName = requiredText(upstreamModelName, "upstreamModelName 不能为空。");
        modelOfferingRepository.findByProviderKeyAndUpstreamModelName(providerKey, normalizedUpstreamModelName)
                .ifPresent(existing -> {
                    if (currentOfferingId == null || existing.id() != currentOfferingId) {
                        throw new BizException(ErrorCode.INVALID_ARGUMENT, "同一供应商下 upstreamModelName 不能重复。");
                    }
                });
    }

    /**
     * 查询模型供应商。
     *
     * @param providerId 供应商主键
     * @return 供应商记录
     */
    private ModelProviderRecord requiredProvider(long providerId) {
        return modelProviderRepository.findById(providerId)
                .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND, "指定模型供应商不存在。"));
    }

    /**
     * 查询模型供应商。
     *
     * @param providerKey 供应商标识
     * @return 供应商记录
     */
    private ModelProviderRecord requiredProvider(String providerKey) {
        return modelProviderRepository.findByProviderKey(providerKey)
                .orElseThrow(() -> new BizException(ErrorCode.INVALID_ARGUMENT, "指定模型供应商不存在。"));
    }

    /**
     * 查询模型供应项。
     *
     * @param offeringId 供应项主键
     * @return 供应项记录
     */
    private ModelOfferingRecord requiredOffering(long offeringId) {
        return modelOfferingRepository.findById(offeringId)
                .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND, "指定模型供应项不存在。"));
    }

    /**
     * 加密 API Key。
     *
     * @param apiKey 明文
     * @return 密文
     */
    private String encrypt(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return "";
        }
        return modelProviderSecretService.encrypt(apiKey.trim());
    }

    /**
     * 生成掩码。
     *
     * @param apiKey 明文
     * @return 掩码
     */
    private String mask(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return "";
        }
        return modelProviderSecretService.mask(apiKey.trim());
    }

    /**
     * 规范化必填文本。
     *
     * @param value 原始值
     * @param message 错误消息
     * @return 规范化文本
     */
    private String requiredText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, message);
        }
        return value.trim();
    }

    /**
     * 规范化可选文本。
     *
     * @param value 原始值
     * @return 规范化文本
     */
    private String normalizedText(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * 规范化温度。
     *
     * @param temperature 温度
     * @return 温度
     */
    private BigDecimal normalizedTemperature(BigDecimal temperature) {
        if (temperature == null) {
            return null;
        }
        if (temperature.compareTo(BigDecimal.ZERO) < 0 || temperature.compareTo(new BigDecimal("2")) > 0) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "defaultTemperature 必须在 0 到 2 之间。");
        }
        return temperature;
    }

    /**
     * 转换为供应商结果。
     *
     * @param record 供应商记录
     * @return 供应商结果
     */
    private ModelProviderResult toProviderResult(ModelProviderRecord record) {
        return new ModelProviderResult(
                record.id(),
                record.providerKey(),
                record.name(),
                record.providerType(),
                record.baseUrl(),
                modelProviderSecretService.isConfigured(record.apiKeyCiphertext()),
                record.apiKeyMask(),
                record.status(),
                record.description(),
                record.createdAt(),
                record.updatedAt()
        );
    }
}
