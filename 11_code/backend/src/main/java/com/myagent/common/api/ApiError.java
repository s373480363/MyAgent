package com.myagent.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 统一错误对象。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "ApiError", description = "统一错误对象。")
public final class ApiError {

    /**
     * 稳定错误码。
     */
    @Schema(description = "稳定错误码。", requiredMode = Schema.RequiredMode.REQUIRED)
    private final String code;

    /**
     * 中文错误消息。
     */
    @Schema(description = "中文错误消息。", requiredMode = Schema.RequiredMode.REQUIRED)
    private final String message;

    /**
     * 结构化错误明细。
     */
    @Schema(description = "结构化错误明细。")
    private final List<Detail> details;

    /**
     * 构造错误对象。
     *
     * @param code 稳定错误码
     * @param message 中文错误消息
     * @param details 结构化错误明细
     */
    public ApiError(String code, String message, List<Detail> details) {
        this.code = code;
        this.message = message;
        this.details = details == null || details.isEmpty() ? null : List.copyOf(details);
    }

    /**
     * 构造错误对象。
     *
     * @param code 稳定错误码
     * @param message 中文错误消息
     * @return 错误对象
     */
    public static ApiError of(String code, String message) {
        return new ApiError(code, message, null);
    }

    /**
     * 构造带明细的错误对象。
     *
     * @param code 稳定错误码
     * @param message 中文错误消息
     * @param details 结构化错误明细
     * @return 错误对象
     */
    public static ApiError of(String code, String message, List<Detail> details) {
        return new ApiError(code, message, details);
    }

    /**
     * 返回稳定错误码。
     *
     * @return 稳定错误码
     */
    public String getCode() {
        return code;
    }

    /**
     * 返回中文错误消息。
     *
     * @return 中文错误消息
     */
    public String getMessage() {
        return message;
    }

    /**
     * 返回结构化错误明细。
     *
     * @return 错误明细
     */
    public List<Detail> getDetails() {
        return details;
    }

    /**
     * 字段级错误明细。
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(name = "ApiErrorDetail", description = "字段级错误明细。")
    public static final class Detail {

        /**
         * 错误字段路径。
         */
        @Schema(description = "错误字段路径。", example = "$.input.question")
        private final String field;

        /**
         * 错误原因代码。
         */
        @Schema(description = "错误原因代码。", example = "required")
        private final String reason;

        /**
         * 中文错误提示。
         */
        @Schema(description = "中文错误提示。", example = "请输入问题")
        private final String message;

        /**
         * 期望值说明。
         */
        @Schema(description = "期望值说明。")
        private final String expected;

        /**
         * 实际值说明。
         */
        @Schema(description = "实际值说明。")
        private final String actual;

        /**
         * 修复建议。
         */
        @Schema(description = "修复建议。")
        private final String hint;

        /**
         * 构造字段级错误明细。
         *
         * @param field 错误字段路径
         * @param reason 错误原因代码
         * @param message 中文错误提示
         * @param expected 期望值说明
         * @param actual 实际值说明
         * @param hint 修复建议
         */
        public Detail(String field, String reason, String message, String expected, String actual, String hint) {
            this.field = field;
            this.reason = reason;
            this.message = message;
            this.expected = expected;
            this.actual = actual;
            this.hint = hint;
        }

        /**
         * 快速构造字段级错误明细。
         *
         * @param field 错误字段路径
         * @param reason 错误原因代码
         * @param message 中文错误提示
         * @return 字段级错误明细
         */
        public static Detail of(String field, String reason, String message) {
            return new Detail(field, reason, message, null, null, null);
        }

        /**
         * 返回错误字段路径。
         *
         * @return 错误字段路径
         */
        public String getField() {
            return field;
        }

        /**
         * 返回错误原因代码。
         *
         * @return 错误原因代码
         */
        public String getReason() {
            return reason;
        }

        /**
         * 返回中文错误提示。
         *
         * @return 中文错误提示
         */
        public String getMessage() {
            return message;
        }

        /**
         * 返回期望值说明。
         *
         * @return 期望值说明
         */
        public String getExpected() {
            return expected;
        }

        /**
         * 返回实际值说明。
         *
         * @return 实际值说明
         */
        public String getActual() {
            return actual;
        }

        /**
         * 返回修复建议。
         *
         * @return 修复建议
         */
        public String getHint() {
            return hint;
        }
    }
}
