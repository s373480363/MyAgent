package com.myagent.schema.domain;

import com.myagent.common.util.CodeEnum;

/**
 * Schema 生命周期状态。
 */
public enum SchemaStatus implements CodeEnum {

    /**
     * 可编辑草稿。
     */
    DRAFT("DRAFT"),

    /**
     * 已被发布工作流引用的活跃版本。
     */
    ACTIVE("ACTIVE"),

    /**
     * 已归档版本。
     */
    ARCHIVED("ARCHIVED");

    /**
     * 稳定状态编码。
     */
    private final String code;

    /**
     * 构造 Schema 状态枚举。
     *
     * @param code 稳定状态编码
     */
    SchemaStatus(String code) {
        this.code = code;
    }

    /**
     * 返回稳定状态编码。
     *
     * @return 稳定状态编码
     */
    @Override
    public String getCode() {
        return code;
    }
}
