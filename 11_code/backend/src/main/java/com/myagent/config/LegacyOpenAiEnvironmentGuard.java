package com.myagent.config;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 启动期 OpenAI 环境变量校验器。
 */
public final class LegacyOpenAiEnvironmentGuard {

    /**
     * 旧变量与对应正式变量映射。
     */
    private static final Map<String, String> LEGACY_TO_FORMAL_ENV = Map.of(
            "OPENAI_API_KEY", "AGENT_STUDIO_OPENAI_API_KEY",
            "SPRING_AI_OPENAI_BASE_URL", "AGENT_STUDIO_OPENAI_BASE_URL",
            "SPRING_AI_OPENAI_API_KEY", "AGENT_STUDIO_OPENAI_API_KEY",
            "SPRING_AI_OPENAI_CHAT_OPTIONS_MODEL", "AGENT_STUDIO_OPENAI_DEFAULT_MODEL",
            "MYAGENT_OPENAI_DEFAULT_MODEL", "AGENT_STUDIO_OPENAI_DEFAULT_MODEL"
    );

    /**
     * 私有构造器。
     */
    private LegacyOpenAiEnvironmentGuard() {
    }

    /**
     * 拒绝旧 OpenAI 变量。
     *
     * @param environment 当前环境变量
     */
    public static void validate(Map<String, String> environment) {
        Map<String, String> detected = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : LEGACY_TO_FORMAL_ENV.entrySet()) {
            String value = environment.get(entry.getKey());
            if (!isBlank(value)) {
                detected.put(entry.getKey(), entry.getValue());
            }
        }
        if (!detected.isEmpty()) {
            StringBuilder message = new StringBuilder("检测到已废弃的 OpenAI 环境变量：");
            boolean first = true;
            for (Map.Entry<String, String> entry : detected.entrySet()) {
                if (!first) {
                    message.append("；");
                }
                message.append(entry.getKey()).append("，请改用 ").append(entry.getValue());
                first = false;
            }
            message.append("。");
            throw new IllegalStateException(message.toString());
        }
    }

    /**
     * 判断字符串是否为空白。
     *
     * @param value 待判断字符串
     * @return 为空白时返回 true
     */
    private static boolean isBlank(String value) {
        return Objects.isNull(value) || value.isBlank();
    }
}
