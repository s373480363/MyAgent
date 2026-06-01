package com.myagent.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.myagent.common.error.BizException;

/**
 * 节点输出已产生但 outputSchema 校验失败的运行时异常。
 */
public class NodeOutputSchemaValidationException extends BizException {

    /**
     * 已产生的节点输出。
     */
    private final JsonNode outputJson;

    /**
     * 构造节点输出 Schema 校验异常。
     *
     * @param outputJson 已产生的节点输出
     * @param cause 原始 Schema 校验异常
     */
    public NodeOutputSchemaValidationException(JsonNode outputJson, BizException cause) {
        super(cause.getErrorCode(), cause.getMessage(), cause.getDetails());
        this.outputJson = outputJson;
        initCause(cause);
    }

    /**
     * 返回已产生的节点输出。
     *
     * @return 节点输出
     */
    public JsonNode getOutputJson() {
        return outputJson;
    }
}
