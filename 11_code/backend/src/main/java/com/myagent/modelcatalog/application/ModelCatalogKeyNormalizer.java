package com.myagent.modelcatalog.application;

import java.util.Locale;

/**
 * 模型目录键规范化工具。
 */
public final class ModelCatalogKeyNormalizer {

    /**
     * 私有构造器。
     */
    private ModelCatalogKeyNormalizer() {
    }

    /**
     * 规范化模型供应项键。
     *
     * @param providerNamespace 供应商命名空间
     * @param modelName 模型名
     * @return 供应项键
     */
    public static String toOfferingKey(String providerNamespace, String modelName) {
        return normalizeSegment(providerNamespace) + "." + normalizeSegment(modelName);
    }

    /**
     * 规范化模型身份键。
     *
     * @param modelName 模型名
     * @return 模型身份键
     */
    public static String toModelKey(String modelName) {
        if (modelName == null || modelName.isBlank()) {
            return "";
        }
        return modelName.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * 规范化键片段。
     *
     * @param raw 原始文本
     * @return 片段
     */
    public static String normalizeSegment(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String lower = raw.trim().toLowerCase(Locale.ROOT);
        String normalized = lower.replaceAll("[^a-z0-9]+", "_");
        normalized = normalized.replaceAll("_+", "_");
        normalized = normalized.replaceAll("^_|_$", "");
        return normalized;
    }
}
