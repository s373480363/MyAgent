package com.myagent.externalagent.domain;

/**
 * 外部 Agent 类型。
 */
public enum ExternalAgentType {

    /**
     * Codex CLI。
     */
    CODEX_CLI,

    /**
     * OpenCode CLI。
     */
    OPENCODE_CLI,

    /**
     * 自定义命令行 Agent。
     */
    CUSTOM_CLI,

    /**
     * 自定义 HTTP Agent。
     */
    CUSTOM_HTTP
}
