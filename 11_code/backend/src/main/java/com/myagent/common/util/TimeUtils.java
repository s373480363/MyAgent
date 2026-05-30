package com.myagent.common.util;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 时间工具。
 */
public final class TimeUtils {

    /**
     * 上海时区。
     */
    public static final ZoneId SHANGHAI_ZONE = ZoneId.of("Asia/Shanghai");

    /**
     * 私有构造，禁止实例化。
     */
    private TimeUtils() {
    }

    /**
     * 返回当前时间。
     *
     * @return 当前时间
     */
    public static Instant now() {
        return Instant.now();
    }

    /**
     * 返回上海时区的当前时间。
     *
     * @return 上海时区当前时间
     */
    public static OffsetDateTime nowInShanghai() {
        return OffsetDateTime.now(SHANGHAI_ZONE);
    }

    /**
     * 将 Instant 格式化为 ISO-8601 字符串。
     *
     * @param instant 时间点
     * @return ISO-8601 字符串
     */
    public static String formatIso(Instant instant) {
        return DateTimeFormatter.ISO_INSTANT.format(instant);
    }
}
