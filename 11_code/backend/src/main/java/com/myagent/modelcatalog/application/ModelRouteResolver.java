package com.myagent.modelcatalog.application;

import com.myagent.common.domain.EnableStatus;
import com.myagent.common.error.BizException;
import com.myagent.common.error.ErrorCode;
import com.myagent.model.ResolvedModelRoute;
import com.myagent.modelcatalog.repository.ModelOfferingRecord;
import com.myagent.modelcatalog.repository.ModelOfferingRepository;
import com.myagent.modelcatalog.repository.ModelProviderRecord;
import com.myagent.modelcatalog.repository.ModelProviderRepository;
import com.myagent.modelcatalog.secret.ModelProviderSecretService;
import org.springframework.stereotype.Component;

/**
 * 模型运行路由解析器。
 */
@Component
public class ModelRouteResolver {

    /**
     * 模型供应项仓储。
     */
    private final ModelOfferingRepository modelOfferingRepository;

    /**
     * 模型供应商仓储。
     */
    private final ModelProviderRepository modelProviderRepository;

    /**
     * 密钥服务。
     */
    private final ModelProviderSecretService modelProviderSecretService;

    /**
     * 构造路由解析器。
     *
     * @param modelOfferingRepository 模型供应项仓储
     * @param modelProviderRepository 模型供应商仓储
     * @param modelProviderSecretService 密钥服务
     */
    public ModelRouteResolver(
            ModelOfferingRepository modelOfferingRepository,
            ModelProviderRepository modelProviderRepository,
            ModelProviderSecretService modelProviderSecretService
    ) {
        this.modelOfferingRepository = modelOfferingRepository;
        this.modelProviderRepository = modelProviderRepository;
        this.modelProviderSecretService = modelProviderSecretService;
    }

    /**
     * 解析正式运行路由。
     *
     * @param modelOfferingKey 供应项标识
     * @return 已解析路由
     */
    public ResolvedModelRoute resolveForInvocation(String modelOfferingKey) {
        ModelOfferingRecord offering = requiredOffering(modelOfferingKey);
        ModelProviderRecord provider = requiredProvider(offering.providerKey());
        if (offering.status() != EnableStatus.ENABLED) {
            throw new BizException(ErrorCode.NODE_EXECUTION_FAILED, "模型供应项已停用，不能执行当前节点。");
        }
        if (provider.status() != EnableStatus.ENABLED) {
            throw new BizException(ErrorCode.NODE_EXECUTION_FAILED, "模型供应商已停用，不能执行当前节点。");
        }
        if (!modelProviderSecretService.isConfigured(provider.apiKeyCiphertext())) {
            throw new BizException(ErrorCode.NODE_EXECUTION_FAILED, "模型供应商尚未配置 API Key。");
        }
        return toRoute(provider, offering);
    }

    /**
     * 解析连接测试路由。
     *
     * @param providerId 供应商主键
     * @param offeringKey 供应项标识
     * @return 已解析路由
     */
    public ResolvedModelRoute resolveForProviderTest(long providerId, String offeringKey) {
        ModelProviderRecord provider = modelProviderRepository.findById(providerId)
                .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND, "指定模型供应商不存在。"));
        ModelOfferingRecord offering = requiredOffering(offeringKey);
        if (!provider.providerKey().equals(offering.providerKey())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "offeringKey 不属于当前模型供应商。");
        }
        if (!modelProviderSecretService.isConfigured(provider.apiKeyCiphertext())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "模型供应商尚未配置 API Key。");
        }
        return toRoute(provider, offering);
    }

    /**
     * 校验供应项可正式运行。
     *
     * @param modelOfferingKey 供应项标识
     */
    public void validatePublishable(String modelOfferingKey) {
        resolveForInvocation(modelOfferingKey);
    }

    /**
     * 按键查询供应项。
     *
     * @param offeringKey 供应项标识
     * @return 供应项记录
     */
    private ModelOfferingRecord requiredOffering(String offeringKey) {
        return modelOfferingRepository.findByOfferingKey(offeringKey)
                .orElseThrow(() -> new BizException(ErrorCode.INVALID_ARGUMENT, "指定模型供应项不存在。"));
    }

    /**
     * 按键查询供应商。
     *
     * @param providerKey 供应商标识
     * @return 供应商记录
     */
    private ModelProviderRecord requiredProvider(String providerKey) {
        return modelProviderRepository.findByProviderKey(providerKey)
                .orElseThrow(() -> new BizException(ErrorCode.INVALID_ARGUMENT, "指定模型供应商不存在。"));
    }

    /**
     * 转换为已解析路由。
     *
     * @param provider 供应商
     * @param offering 供应项
     * @return 已解析路由
     */
    private ResolvedModelRoute toRoute(ModelProviderRecord provider, ModelOfferingRecord offering) {
        return new ResolvedModelRoute(
                provider.id(),
                provider.providerKey(),
                provider.name(),
                provider.baseUrl(),
                modelProviderSecretService.decrypt(provider.apiKeyCiphertext()),
                offering.id(),
                offering.offeringKey(),
                offering.modelKey(),
                offering.upstreamModelName(),
                offering.defaultTemperature()
        );
    }
}
