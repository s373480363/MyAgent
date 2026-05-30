package com.myagent.common.util;

import java.util.Arrays;
import java.util.Objects;

/**
 * 枚举转换工具。
 */
public final class EnumUtils {

    /**
     * 私有构造，禁止实例化。
     */
    private EnumUtils() {
    }

    /**
     * 根据稳定代码查找枚举值。
     *
     * @param enumType 枚举类型
     * @param code 稳定代码
     * @param <E> 枚举类型
     * @return 枚举值
     */
    public static <E extends Enum<E> & CodeEnum> E fromCode(Class<E> enumType, String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return Arrays.stream(enumType.getEnumConstants())
                .filter(value -> Objects.equals(value.getCode(), code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知枚举代码：" + code));
    }

    /**
     * 判断稳定代码是否存在。
     *
     * @param enumType 枚举类型
     * @param code 稳定代码
     * @param <E> 枚举类型
     * @return 存在时返回 true
     */
    public static <E extends Enum<E> & CodeEnum> boolean containsCode(Class<E> enumType, String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        return Arrays.stream(enumType.getEnumConstants())
                .anyMatch(value -> Objects.equals(value.getCode(), code));
    }
}
