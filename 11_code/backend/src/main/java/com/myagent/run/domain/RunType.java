package com.myagent.run.domain;

import com.myagent.common.util.CodeEnum;

/**
 * 运行类型。
 */
public enum RunType implements CodeEnum {

    /**
     * 调试运行。
     */
    DEBUG("DEBUG"),

    /**
     * 对外 API 正式运行。
     */
    API("API"),

    /**
     * 内部 Agent 调用子运行。
     */
    AGENT_CALL("AGENT_CALL"),

    /**
     * 节点验收配套运行。
     */
    EVAL("EVAL");

    /**
     * 稳定类型码。
     */
    private final String code;

    /**
     * 构造运行类型。
     *
     * @param code 稳定类型码
     */
    RunType(String code) {
        this.code = code;
    }

    /**
     * 返回稳定类型码。
     *
     * @return 稳定类型码
     */
    @Override
    public String getCode() {
        return code;
    }
}
