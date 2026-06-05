package com.myagent.model;

/**
 * OpenAI 模型网关。
 */
public interface OpenAiModelGateway {

    /**
     * 构造模型请求 Trace 白名单载荷。
     *
     * @param request 模型调用请求
     * @return Trace 白名单载荷
     */
    ModelRequestTracePayload resolveRequestTracePayload(ModelInvocationRequest request);

    /**
     * 调用模型。
     *
     * @param request 模型调用请求
     * @return 模型调用结果
     */
    ModelInvocationResult invoke(ModelInvocationRequest request);
}
