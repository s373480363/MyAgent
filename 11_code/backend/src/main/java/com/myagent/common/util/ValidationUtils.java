package com.myagent.common.util;

/**
 * 公共校验工具。
 */
public final class ValidationUtils {

    /**
     * 私有构造，禁止实例化。
     */
    private ValidationUtils() {
    }

    /**
     * 校验对象不能为空。
     *
     * @param value 待校验对象
     * @param fieldName 字段名
     * @param <T> 对象类型
     * @return 原始对象
     */
    public static <T> T requireNonNull(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " 不能为空。");
        }
        return value;
    }

    /**
     * 校验字符串不能为空白。
     *
     * @param value 待校验字符串
     * @param fieldName 字段名
     * @return 原始字符串
     */
    public static String requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " 不能为空。");
        }
        return value;
    }

    /**
     * 校验整数必须大于 0。
     *
     * @param value 待校验整数
     * @param fieldName 字段名
     * @return 原始整数
     */
    public static long requirePositive(long value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " 必须大于 0。");
        }
        return value;
    }

    /**
     * 校验整数必须大于 0。
     *
     * @param value 待校验整数
     * @param fieldName 字段名
     * @return 原始整数
     */
    public static int requirePositive(int value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " 必须大于 0。");
        }
        return value;
    }
}
