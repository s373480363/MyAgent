package com.myagent.modelcatalog.application;

import java.util.Locale;

/**
 * 模型目录默认初始化规则。
 */
public final class ModelCatalogBootstrapDefaults {

    /**
     * 默认供应商主键。
     */
    public static final String DEFAULT_PROVIDER_KEY = "openai-default";

    /**
     * 默认供应商显示名称。
     */
    public static final String DEFAULT_PROVIDER_NAME = "默认 OpenAI-compatible 供应商";

    /**
     * 默认基础地址。
     */
    public static final String DEFAULT_BASE_URL = "https://api.openai.com";

    /**
     * 默认供应项命名空间。
     */
    public static final String DEFAULT_PROVIDER_NAMESPACE = "openai";

    /**
     * 私有构造器。
     */
    private ModelCatalogBootstrapDefaults() {
    }

    /**
     * 规范化默认基础地址。
     *
     * @param rawBaseUrl 原始基础地址
     * @return 基础地址
     */
    public static String normalizeBaseUrl(String rawBaseUrl) {
        if (rawBaseUrl == null || rawBaseUrl.isBlank()) {
            return DEFAULT_BASE_URL;
        }
        return rawBaseUrl.trim();
    }

    /**
     * 规范化默认模型名。
     *
     * @param rawModel 原始模型名
     * @return 模型名
     */
    public static String normalizeModel(String rawModel) {
        if (rawModel == null || rawModel.isBlank()) {
            return "gpt-4.1-mini";
        }
        return rawModel.trim().toLowerCase(Locale.ROOT);
    }
}
