package com.myagent.schema.validation;

import com.myagent.common.util.CodeEnum;

/**
 * Schema 校验触发阶段。
 */
public enum ValidationStage implements CodeEnum {

    /**
     * Schema 定义保存前。
     */
    SCHEMA_DEFINITION("SCHEMA_DEFINITION"),

    /**
     * START 节点输入。
     */
    START_INPUT("START_INPUT"),

    /**
     * 节点输入。
     */
    NODE_INPUT("NODE_INPUT"),

    /**
     * 节点输出。
     */
    NODE_OUTPUT("NODE_OUTPUT"),

    /**
     * 最终输出。
     */
    END_OUTPUT("END_OUTPUT"),

    /**
     * 工作流发布校验。
     */
    WORKFLOW_PUBLISH("WORKFLOW_PUBLISH");

    /**
     * 稳定阶段编码。
     */
    private final String code;

    /**
     * 构造校验阶段。
     *
     * @param code 稳定阶段编码
     */
    ValidationStage(String code) {
        this.code = code;
    }

    /**
     * 返回稳定阶段编码。
     *
     * @return 稳定阶段编码
     */
    @Override
    public String getCode() {
        return code;
    }
}
