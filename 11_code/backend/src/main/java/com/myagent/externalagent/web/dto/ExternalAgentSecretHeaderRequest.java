package com.myagent.externalagent.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 外部 Agent 敏感 header 请求项。
 */
public final class ExternalAgentSecretHeaderRequest {

    /**
     * header 名称。
     */
    @NotBlank(message = "headerName 不能为空。")
    private String headerName;

    /**
     * 只写 secret 值。
     */
    private String secretValue;

    /**
     * 返回 header 名称。
     *
     * @return header 名称
     */
    public String getHeaderName() {
        return headerName;
    }

    /**
     * 设置 header 名称。
     *
     * @param headerName header 名称
     */
    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }

    /**
     * 返回 secret 值。
     *
     * @return secret 值
     */
    public String getSecretValue() {
        return secretValue;
    }

    /**
     * 设置 secret 值。
     *
     * @param secretValue secret 值
     */
    public void setSecretValue(String secretValue) {
        this.secretValue = secretValue;
    }
}
