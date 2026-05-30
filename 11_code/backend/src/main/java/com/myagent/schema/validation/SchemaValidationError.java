package com.myagent.schema.validation;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Schema 字段级校验错误。
 */
@Schema(name = "SchemaValidationError", description = "Schema 字段级校验错误。")
public final class SchemaValidationError {

    /**
     * JSON 字段路径。
     */
    @Schema(description = "JSON 字段路径。", example = "$.input.question")
    private final String path;

    /**
     * JSON Schema 关键字。
     */
    @Schema(description = "JSON Schema 关键字。", example = "required")
    private final String keyword;

    /**
     * 中文错误消息。
     */
    @Schema(description = "中文错误消息。", example = "请输入问题。")
    private final String message;

    /**
     * 构造字段级校验错误。
     *
     * @param path JSON 字段路径
     * @param keyword JSON Schema 关键字
     * @param message 中文错误消息
     */
    public SchemaValidationError(String path, String keyword, String message) {
        this.path = path;
        this.keyword = keyword;
        this.message = message;
    }

    /**
     * 返回 JSON 字段路径。
     *
     * @return JSON 字段路径
     */
    public String getPath() {
        return path;
    }

    /**
     * 返回 JSON Schema 关键字。
     *
     * @return JSON Schema 关键字
     */
    public String getKeyword() {
        return keyword;
    }

    /**
     * 返回中文错误消息。
     *
     * @return 中文错误消息
     */
    public String getMessage() {
        return message;
    }
}
