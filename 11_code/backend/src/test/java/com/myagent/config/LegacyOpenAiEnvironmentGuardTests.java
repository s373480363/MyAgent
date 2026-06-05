package com.myagent.config;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 旧 OpenAI 环境变量拒绝器测试。
 */
class LegacyOpenAiEnvironmentGuardTests {

    /**
     * 未出现旧变量时应直接放行。
     */
    @Test
    void validateAllowsFormalEnvironmentVariables() {
        assertThatCode(() -> LegacyOpenAiEnvironmentGuard.validate(Map.of(
                "AGENT_STUDIO_OPENAI_API_KEY", "new-key",
                "AGENT_STUDIO_OPENAI_BASE_URL", "https://api.openai.com",
                "AGENT_STUDIO_OPENAI_DEFAULT_MODEL", "gpt-4.1-mini"
        ))).doesNotThrowAnyException();
    }

    /**
     * 出现旧变量时必须直接失败，并提示对应正式变量名。
     */
    @Test
    void validateRejectsLegacyEnvironmentVariables() {
        assertThatThrownBy(() -> LegacyOpenAiEnvironmentGuard.validate(Map.of(
                "OPENAI_API_KEY", "legacy-key",
                "MYAGENT_OPENAI_DEFAULT_MODEL", "gpt-4.1-mini"
        ))).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OPENAI_API_KEY")
                .hasMessageContaining("AGENT_STUDIO_OPENAI_API_KEY")
                .hasMessageContaining("MYAGENT_OPENAI_DEFAULT_MODEL")
                .hasMessageContaining("AGENT_STUDIO_OPENAI_DEFAULT_MODEL");
    }

    /**
     * 缺失正式 OpenAI Key 时应放行；运行时不再把旧 OpenAI 环境变量作为正式事实源。
     */
    @Test
    void validateAllowsMissingFormalOpenAiApiKey() {
        assertThatCode(() -> LegacyOpenAiEnvironmentGuard.validate(Map.of(
                "AGENT_STUDIO_OPENAI_BASE_URL", "https://api.openai.com",
                "AGENT_STUDIO_OPENAI_DEFAULT_MODEL", "gpt-4.1-mini"
        ))).doesNotThrowAnyException();
    }

    /**
     * 正式 OpenAI Key 为空白时也应放行；是否使用目录密钥由数据库配置决定。
     */
    @Test
    void validateAllowsBlankFormalOpenAiApiKey() {
        assertThatCode(() -> LegacyOpenAiEnvironmentGuard.validate(Map.of(
                "AGENT_STUDIO_OPENAI_API_KEY", "   ",
                "AGENT_STUDIO_OPENAI_BASE_URL", "https://api.openai.com",
                "AGENT_STUDIO_OPENAI_DEFAULT_MODEL", "gpt-4.1-mini"
        ))).doesNotThrowAnyException();
    }
}
