package com.myagent.run.domain;

import com.myagent.common.util.CodeEnum;

/**
 * 运行状态。
 */
public enum RunStatus implements CodeEnum {

    /**
     * 运行记录已创建。
     */
    PENDING("PENDING"),

    /**
     * 运行中。
     */
    RUNNING("RUNNING"),

    /**
     * 运行成功。
     */
    SUCCESS("SUCCESS"),

    /**
     * 业务执行失败。
     */
    FAILED("FAILED"),

    /**
     * 运行或节点超时。
     */
    TIMEOUT("TIMEOUT"),

    /**
     * 外部中断或父运行级联取消。
     */
    CANCELED("CANCELED");

    /**
     * 稳定状态码。
     */
    private final String code;

    /**
     * 构造运行状态。
     *
     * @param code 稳定状态码
     */
    RunStatus(String code) {
        this.code = code;
    }

    /**
     * 返回稳定状态码。
     *
     * @return 稳定状态码
     */
    @Override
    public String getCode() {
        return code;
    }
}
