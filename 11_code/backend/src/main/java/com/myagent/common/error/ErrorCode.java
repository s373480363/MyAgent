package com.myagent.common.error;

import com.myagent.common.util.CodeEnum;

/**
 * 平台统一错误码。
 */
public enum ErrorCode implements CodeEnum {

    /**
     * 请求参数不合法。
     */
    INVALID_ARGUMENT("INVALID_ARGUMENT", "请求参数不合法。"),

    /**
     * 请求体无法解析。
     */
    INVALID_REQUEST_BODY("INVALID_REQUEST_BODY", "请求体格式不正确。"),

    /**
     * 资源不存在。
     */
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", "请求的资源不存在。"),

    /**
     * Agent 不存在。
     */
    AGENT_NOT_FOUND("AGENT_NOT_FOUND", "指定 Agent 不存在。"),

    /**
     * Agent 已停用。
     */
    AGENT_DISABLED("AGENT_DISABLED", "Agent 已停用，不能执行当前操作。"),

    /**
     * 工作流校验失败。
     */
    WORKFLOW_VALIDATION_FAILED("WORKFLOW_VALIDATION_FAILED", "工作流校验失败，请修正后重试。"),

    /**
     * Schema 校验失败。
     */
    SCHEMA_VALIDATION_FAILED("SCHEMA_VALIDATION_FAILED", "Schema 校验失败。"),

    /**
     * 节点执行失败。
     */
    NODE_EXECUTION_FAILED("NODE_EXECUTION_FAILED", "节点执行失败。"),

    /**
     * 运行超时。
     */
    RUN_TIMEOUT("RUN_TIMEOUT", "运行超时。"),

    /**
     * 运行被取消。
     */
    RUN_CANCELED("RUN_CANCELED", "运行被取消。"),

    /**
     * Java 方法执行失败。
     */
    JAVA_METHOD_EXECUTION_FAILED("JAVA_METHOD_EXECUTION_FAILED", "Java 方法执行失败。"),

    /**
     * 工具调用失败。
     */
    TOOL_CALL_FAILED("TOOL_CALL_FAILED", "工具调用失败。"),

    /**
     * 外部 Agent 调用失败。
     */
    EXTERNAL_AGENT_CALL_FAILED("EXTERNAL_AGENT_CALL_FAILED", "外部 Agent 调用失败。"),

    /**
     * 目标 Agent 未发布。
     */
    TARGET_AGENT_NOT_PUBLISHED("TARGET_AGENT_NOT_PUBLISHED", "目标 Agent 尚未发布，不能作为子 Agent 调用。"),

    /**
     * 验收用例未确认。
     */
    EVAL_CASE_UNCONFIRMED("EVAL_CASE_UNCONFIRMED", "验收用例尚未确认，不能参与正式验收。"),

    /**
     * 验收判定失败。
     */
    EVAL_JUDGE_RULE_FAILED("EVAL_JUDGE_RULE_FAILED", "验收判定失败。"),

    /**
     * 系统设置不可编辑。
     */
    SETTING_NOT_EDITABLE("SETTING_NOT_EDITABLE", "该系统设置不可编辑。"),

    /**
     * 系统内部错误。
     */
    INTERNAL_ERROR("INTERNAL_ERROR", "系统内部异常，请稍后重试。");

    /**
     * 稳定机器错误码。
     */
    private final String code;

    /**
     * 默认中文错误消息。
     */
    private final String defaultMessage;

    /**
     * 构造错误码。
     *
     * @param code 稳定机器错误码
     * @param defaultMessage 默认中文错误消息
     */
    ErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    /**
     * 返回稳定机器错误码。
     *
     * @return 稳定机器错误码
     */
    public String getCode() {
        return code;
    }

    /**
     * 返回默认中文错误消息。
     *
     * @return 默认中文错误消息
     */
    public String getDefaultMessage() {
        return defaultMessage;
    }
}
