package com.myagent.model;

import com.myagent.common.error.BizException;
import com.myagent.common.error.ErrorCode;
import com.myagent.modelcatalog.application.ModelRouteResolver;
import org.springframework.stereotype.Component;

/**
 * 默认 OpenAI 模型网关。
 */
@Component
public class DefaultOpenAiModelGateway implements OpenAiModelGateway {

    /**
     * 模型运行路由解析器。
     */
    private final ModelRouteResolver modelRouteResolver;

    /**
     * OpenAI-compatible 模型调用器。
     */
    private final OpenAiCompatibleModelInvoker openAiCompatibleModelInvoker;

    /**
     * 构造模型网关。
     *
     * @param modelRouteResolver 模型运行路由解析器
     * @param openAiCompatibleModelInvoker OpenAI-compatible 模型调用器
     */
    public DefaultOpenAiModelGateway(
            ModelRouteResolver modelRouteResolver,
            OpenAiCompatibleModelInvoker openAiCompatibleModelInvoker
    ) {
        this.modelRouteResolver = modelRouteResolver;
        this.openAiCompatibleModelInvoker = openAiCompatibleModelInvoker;
    }

    @Override
    public ModelRequestTracePayload resolveRequestTracePayload(ModelInvocationRequest request) {
        ResolvedModelRoute route = resolveRoute(request);
        return new ModelRequestTracePayload(
                route.providerKey(),
                route.providerName(),
                route.offeringKey(),
                route.modelKey(),
                route.upstreamModelName(),
                resolvedTemperature(request, route),
                request.structuredOutput()
        );
    }

    /**
     * 调用模型。
     *
     * @param request 模型调用请求
     * @return 模型调用结果
     */
    @Override
    public ModelInvocationResult invoke(ModelInvocationRequest request) {
        return openAiCompatibleModelInvoker.invoke(resolveRoute(request), request);
    }

    /**
     * 解析正式运行路由。
     *
     * @param request 模型调用请求
     * @return 已解析路由
     */
    private ResolvedModelRoute resolveRoute(ModelInvocationRequest request) {
        if (request == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "模型调用请求不能为空。");
        }
        if (request.modelOfferingKey() == null || request.modelOfferingKey().isBlank()) {
            throw new BizException(ErrorCode.NODE_EXECUTION_FAILED, "模型供应项未配置，无法执行当前模型调用。");
        }
        return modelRouteResolver.resolveForInvocation(request.modelOfferingKey().trim());
    }

    /**
     * 计算最终温度。
     *
     * @param request 模型调用请求
     * @param route 已解析路由
     * @return 最终温度
     */
    private java.math.BigDecimal resolvedTemperature(ModelInvocationRequest request, ResolvedModelRoute route) {
        return request.temperature() == null ? route.defaultTemperature() : request.temperature();
    }
}
