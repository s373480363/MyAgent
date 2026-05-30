package com.myagent.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * JSON 工具。
 */
public final class JsonUtils {

    /**
     * 私有构造，禁止实例化。
     */
    private JsonUtils() {
    }

    /**
     * 转换为 JsonNode。
     *
     * @param objectMapper Jackson 对象映射器
     * @param value 原始对象
     * @return JSON 节点
     */
    public static JsonNode toJsonNode(ObjectMapper objectMapper, Object value) {
        return objectMapper.valueToTree(value);
    }

    /**
     * 转换对象类型。
     *
     * @param objectMapper Jackson 对象映射器
     * @param value 原始对象
     * @param targetType 目标类型
     * @param <T> 目标类型
     * @return 转换后的对象
     */
    public static <T> T convert(ObjectMapper objectMapper, Object value, Class<T> targetType) {
        return objectMapper.convertValue(value, targetType);
    }

    /**
     * 以漂亮格式输出 JSON。
     *
     * @param objectMapper Jackson 对象映射器
     * @param value 原始对象
     * @return 美化后的 JSON 字符串
     */
    public static String prettyString(ObjectMapper objectMapper, Object value) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("JSON 序列化失败。", exception);
        }
    }
}
