package com.myagent.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * MyAgent 白名单设置启动配置。
 */
@Component
@Validated
@ConfigurationProperties(prefix = "myagent")
public class MyAgentSettingsProperties {

    /**
     * OpenAI 配置。
     */
    private final Openai openai = new Openai();

    /**
     * 运行时配置。
     */
    private final Runtime runtime = new Runtime();

    /**
     * 返回 OpenAI 配置。
     *
     * @return OpenAI 配置
     */
    public Openai getOpenai() {
        return openai;
    }

    /**
     * 返回运行时配置。
     *
     * @return 运行时配置
     */
    public Runtime getRuntime() {
        return runtime;
    }

    /**
     * OpenAI 配置对象。
     */
    public static class Openai {

        /**
         * 默认模型。
         */
        @NotBlank
        private String defaultModel = "gpt-4.1-mini";

        /**
         * 返回默认模型。
         *
         * @return 默认模型
         */
        public String getDefaultModel() {
            return defaultModel;
        }

        /**
         * 设置默认模型。
         *
         * @param defaultModel 默认模型
         */
        public void setDefaultModel(String defaultModel) {
            this.defaultModel = defaultModel;
        }
    }

    /**
     * 运行时配置对象。
     */
    public static class Runtime {

        /**
         * Agent 默认总超时。
         */
        @Min(1)
        private int defaultAgentTimeoutSeconds = 600;

        /**
         * LLM 节点默认超时。
         */
        @Min(1)
        private int defaultLlmTimeoutSeconds = 120;

        /**
         * Java 方法节点默认超时。
         */
        @Min(1)
        private int defaultJavaMethodTimeoutSeconds = 30;

        /**
         * 外部 Agent 节点默认超时。
         */
        @Min(1)
        private int defaultExternalAgentTimeoutSeconds = 600;

        /**
         * 默认最大步数。
         */
        @Min(1)
        private int defaultMaxSteps = 30;

        /**
         * 默认最大 Agent 调用深度。
         */
        @Min(1)
        private int defaultMaxAgentCallDepth = 3;

        /**
         * 返回 Agent 默认总超时。
         *
         * @return Agent 默认总超时
         */
        public int getDefaultAgentTimeoutSeconds() {
            return defaultAgentTimeoutSeconds;
        }

        /**
         * 设置 Agent 默认总超时。
         *
         * @param defaultAgentTimeoutSeconds Agent 默认总超时
         */
        public void setDefaultAgentTimeoutSeconds(int defaultAgentTimeoutSeconds) {
            this.defaultAgentTimeoutSeconds = defaultAgentTimeoutSeconds;
        }

        /**
         * 返回 LLM 节点默认超时。
         *
         * @return LLM 节点默认超时
         */
        public int getDefaultLlmTimeoutSeconds() {
            return defaultLlmTimeoutSeconds;
        }

        /**
         * 设置 LLM 节点默认超时。
         *
         * @param defaultLlmTimeoutSeconds LLM 节点默认超时
         */
        public void setDefaultLlmTimeoutSeconds(int defaultLlmTimeoutSeconds) {
            this.defaultLlmTimeoutSeconds = defaultLlmTimeoutSeconds;
        }

        /**
         * 返回 Java 方法节点默认超时。
         *
         * @return Java 方法节点默认超时
         */
        public int getDefaultJavaMethodTimeoutSeconds() {
            return defaultJavaMethodTimeoutSeconds;
        }

        /**
         * 设置 Java 方法节点默认超时。
         *
         * @param defaultJavaMethodTimeoutSeconds Java 方法节点默认超时
         */
        public void setDefaultJavaMethodTimeoutSeconds(int defaultJavaMethodTimeoutSeconds) {
            this.defaultJavaMethodTimeoutSeconds = defaultJavaMethodTimeoutSeconds;
        }

        /**
         * 返回外部 Agent 节点默认超时。
         *
         * @return 外部 Agent 节点默认超时
         */
        public int getDefaultExternalAgentTimeoutSeconds() {
            return defaultExternalAgentTimeoutSeconds;
        }

        /**
         * 设置外部 Agent 节点默认超时。
         *
         * @param defaultExternalAgentTimeoutSeconds 外部 Agent 节点默认超时
         */
        public void setDefaultExternalAgentTimeoutSeconds(int defaultExternalAgentTimeoutSeconds) {
            this.defaultExternalAgentTimeoutSeconds = defaultExternalAgentTimeoutSeconds;
        }

        /**
         * 返回默认最大步数。
         *
         * @return 默认最大步数
         */
        public int getDefaultMaxSteps() {
            return defaultMaxSteps;
        }

        /**
         * 设置默认最大步数。
         *
         * @param defaultMaxSteps 默认最大步数
         */
        public void setDefaultMaxSteps(int defaultMaxSteps) {
            this.defaultMaxSteps = defaultMaxSteps;
        }

        /**
         * 返回默认最大 Agent 调用深度。
         *
         * @return 默认最大 Agent 调用深度
         */
        public int getDefaultMaxAgentCallDepth() {
            return defaultMaxAgentCallDepth;
        }

        /**
         * 设置默认最大 Agent 调用深度。
         *
         * @param defaultMaxAgentCallDepth 默认最大 Agent 调用深度
         */
        public void setDefaultMaxAgentCallDepth(int defaultMaxAgentCallDepth) {
            this.defaultMaxAgentCallDepth = defaultMaxAgentCallDepth;
        }
    }
}
