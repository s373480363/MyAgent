package com.myagent.common.error;

import com.myagent.common.api.ApiError;

import java.util.List;

/**
 * 统一业务异常。
 */
public class BizException extends RuntimeException {

    /**
     * 错误码定义。
     */
    private final ErrorCode errorCode;

    /**
     * 结构化错误明细。
     */
    private final List<ApiError.Detail> details;

    /**
     * 使用默认错误消息构造业务异常。
     *
     * @param errorCode 错误码定义
     */
    public BizException(ErrorCode errorCode) {
        this(errorCode, errorCode.getDefaultMessage(), null);
    }

    /**
     * 使用自定义错误消息构造业务异常。
     *
     * @param errorCode 错误码定义
     * @param message 自定义中文错误消息
     */
    public BizException(ErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    /**
     * 使用完整参数构造业务异常。
     *
     * @param errorCode 错误码定义
     * @param message 自定义中文错误消息
     * @param details 结构化错误明细
     */
    public BizException(ErrorCode errorCode, String message, List<ApiError.Detail> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details == null || details.isEmpty() ? null : List.copyOf(details);
    }

    /**
     * 返回错误码定义。
     *
     * @return 错误码定义
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * 返回结构化错误明细。
     *
     * @return 结构化错误明细
     */
    public List<ApiError.Detail> getDetails() {
        return details;
    }
}
