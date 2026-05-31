package com.myagent.run.domain;

import com.myagent.common.util.CodeEnum;

/**
 * Trace 事件类型。
 */
public enum TraceEventType implements CodeEnum {

    /**
     * 模型请求事件。
     */
    MODEL_REQUEST("MODEL_REQUEST"),

    /**
     * 模型响应事件。
     */
    MODEL_RESPONSE("MODEL_RESPONSE"),

    /**
     * Schema 校验事件。
     */
    SCHEMA_VALIDATION("SCHEMA_VALIDATION"),

    /**
     * 条件分支决策事件。
     */
    CONDITION_DECISION("CONDITION_DECISION"),

    /**
     * Java 方法调用事件。
     */
    JAVA_METHOD_CALL("JAVA_METHOD_CALL"),

    /**
     * 工具调用事件。
     */
    TOOL_CALL("TOOL_CALL"),

    /**
     * 外部 Agent 调用事件。
     */
    EXTERNAL_AGENT_CALL("EXTERNAL_AGENT_CALL"),

    /**
     * 内部 Agent 调用事件。
     */
    AGENT_CALL("AGENT_CALL"),

    /**
     * 验收用例结果事件。
     */
    EVAL_CASE_RESULT("EVAL_CASE_RESULT"),

    /**
     * 节点错误事件。
     */
    NODE_ERROR("NODE_ERROR"),

    /**
     * 运行完成事件。
     */
    RUN_FINISHED("RUN_FINISHED");

    /**
     * 稳定事件码。
     */
    private final String code;

    /**
     * 构造 Trace 事件类型。
     *
     * @param code 稳定事件码
     */
    TraceEventType(String code) {
        this.code = code;
    }

    /**
     * 返回稳定事件码。
     *
     * @return 稳定事件码
     */
    @Override
    public String getCode() {
        return code;
    }
}
