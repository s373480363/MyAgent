package com.myagent.schema.domain;

import com.myagent.common.util.CodeEnum;

/**
 * Schema 定义来源。
 */
public enum SchemaCreatedFrom implements CodeEnum {

    /**
     * 用户手工创建。
     */
    USER_CREATED("USER_CREATED"),

    /**
     * Java 方法扫描生成。
     */
    JAVA_METHOD_SCAN("JAVA_METHOD_SCAN"),

    /**
     * 工具定义生成。
     */
    TOOL_DEFINITION("TOOL_DEFINITION"),

    /**
     * 系统内置。
     */
    SYSTEM_BUILTIN("SYSTEM_BUILTIN"),

    /**
     * Agent 输入契约。
     */
    AGENT_INPUT("AGENT_INPUT"),

    /**
     * Agent 输出契约。
     */
    AGENT_OUTPUT("AGENT_OUTPUT");

    /**
     * 稳定来源编码。
     */
    private final String code;

    /**
     * 构造 Schema 来源枚举。
     *
     * @param code 稳定来源编码
     */
    SchemaCreatedFrom(String code) {
        this.code = code;
    }

    /**
     * 返回稳定来源编码。
     *
     * @return 稳定来源编码
     */
    @Override
    public String getCode() {
        return code;
    }
}
