package com.myagent.modelcatalog.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 模型目录默认初始化规则测试。
 */
class ModelCatalogBootstrapDefaultsTests {

    /**
     * 未提供基础地址时应回退到正式默认地址。
     */
    @Test
    void normalizeBaseUrlFallsBackToDefaultWhenBlank() {
        assertThat(ModelCatalogBootstrapDefaults.normalizeBaseUrl("  "))
                .isEqualTo(ModelCatalogBootstrapDefaults.DEFAULT_BASE_URL);
    }

    /**
     * 普通 OpenAI-compatible 地址应保持不变。
     */
    @Test
    void normalizeBaseUrlKeepsNormalCompatibleUrl() {
        assertThat(ModelCatalogBootstrapDefaults.normalizeBaseUrl("https://api.openai.com"))
                .isEqualTo("https://api.openai.com");
    }

    /**
     * Poe 根地址初始化时不应保留 /v1，避免 Spring AI 再次拼接版本路径。
     */
    @Test
    void normalizeBaseUrlRemovesPoeVersionSuffix() {
        assertThat(ModelCatalogBootstrapDefaults.normalizeBaseUrl("https://api.poe.com/v1"))
                .isEqualTo("https://api.poe.com");
        assertThat(ModelCatalogBootstrapDefaults.normalizeBaseUrl("https://api.poe.com/v1/"))
                .isEqualTo("https://api.poe.com");
    }
}
