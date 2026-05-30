package com.myagent.schema.validation;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Schema 校验结果。
 */
@Schema(name = "SchemaValidationResult", description = "Schema 校验结果。")
public final class SchemaValidationResult {

    /**
     * 是否通过校验。
     */
    @Schema(description = "是否通过校验。", requiredMode = Schema.RequiredMode.REQUIRED)
    private final boolean valid;

    /**
     * Schema 业务标识。
     */
    @Schema(description = "Schema 业务标识。", example = "agent.input")
    private final String schemaKey;

    /**
     * Schema 版本。
     */
    @Schema(description = "Schema 版本。", example = "1")
    private final Integer schemaVersion;

    /**
     * 字段级错误。
     */
    @Schema(description = "字段级错误。")
    private final List<SchemaValidationError> errors;

    /**
     * 构造 Schema 校验结果。
     *
     * @param valid 是否通过校验
     * @param schemaKey Schema 业务标识
     * @param schemaVersion Schema 版本
     * @param errors 字段级错误
     */
    public SchemaValidationResult(
            boolean valid,
            String schemaKey,
            Integer schemaVersion,
            List<SchemaValidationError> errors
    ) {
        this.valid = valid;
        this.schemaKey = schemaKey;
        this.schemaVersion = schemaVersion;
        this.errors = errors == null || errors.isEmpty() ? List.of() : List.copyOf(errors);
    }

    /**
     * 构造成功校验结果。
     *
     * @param schemaKey Schema 业务标识
     * @param schemaVersion Schema 版本
     * @return 成功校验结果
     */
    public static SchemaValidationResult valid(String schemaKey, Integer schemaVersion) {
        return new SchemaValidationResult(true, schemaKey, schemaVersion, List.of());
    }

    /**
     * 构造失败校验结果。
     *
     * @param schemaKey Schema 业务标识
     * @param schemaVersion Schema 版本
     * @param errors 字段级错误
     * @return 失败校验结果
     */
    public static SchemaValidationResult invalid(
            String schemaKey,
            Integer schemaVersion,
            List<SchemaValidationError> errors
    ) {
        return new SchemaValidationResult(false, schemaKey, schemaVersion, errors);
    }

    /**
     * 返回是否通过校验。
     *
     * @return 是否通过校验
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * 返回 Schema 业务标识。
     *
     * @return Schema 业务标识
     */
    public String getSchemaKey() {
        return schemaKey;
    }

    /**
     * 返回 Schema 版本。
     *
     * @return Schema 版本
     */
    public Integer getSchemaVersion() {
        return schemaVersion;
    }

    /**
     * 返回字段级错误。
     *
     * @return 字段级错误
     */
    public List<SchemaValidationError> getErrors() {
        return errors;
    }
}
