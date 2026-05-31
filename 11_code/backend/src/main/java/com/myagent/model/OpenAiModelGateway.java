package com.myagent.model;

/**
 * OpenAI 模型网关。
 */
public interface OpenAiModelGateway {

    /**
     * 调用模型。
     *
     * @param request 模型调用请求
     * @return 模型调用结果
     */
    ModelInvocationResult invoke(ModelInvocationRequest request);
}
