package com.myagent.externalagent.web.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 更新外部 Agent 请求。
 */
public final class UpdateExternalAgentRequest {

    /**
     * 名称。
     */
    @NotBlank(message = "name 不能为空。")
    private String name;

    /**
     * 描述。
     */
    private String description;

    /**
     * 结构化命令配置。
     */
    @NotNull(message = "commandJson 不能为空。")
    private JsonNode commandJson;

    /**
     * 敏感 header 定义。
     */
    @Valid
    private List<ExternalAgentSecretHeaderRequest> secretHeaders;

    /**
     * 工作目录。
     */
    private String workingDirectory;

    /**
     * 超时时间。
     */
    @Min(value = 1, message = "timeoutSeconds 必须大于 0。")
    private Integer timeoutSeconds;

    /**
     * 是否采集 stdout。
     */
    private Boolean captureStdout;

    /**
     * 是否采集 stderr。
     */
    private Boolean captureStderr;

    /**
     * 是否采集 Git diff。
     */
    private Boolean captureGitDiff;

    /**
     * 输出 Schema 主键。
     */
    @Min(value = 1, message = "outputSchemaId 必须大于 0。")
    private Long outputSchemaId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public JsonNode getCommandJson() {
        return commandJson;
    }

    public void setCommandJson(JsonNode commandJson) {
        this.commandJson = commandJson;
    }

    public List<ExternalAgentSecretHeaderRequest> getSecretHeaders() {
        return secretHeaders;
    }

    public void setSecretHeaders(List<ExternalAgentSecretHeaderRequest> secretHeaders) {
        this.secretHeaders = secretHeaders;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public Boolean getCaptureStdout() {
        return captureStdout;
    }

    public void setCaptureStdout(Boolean captureStdout) {
        this.captureStdout = captureStdout;
    }

    public Boolean getCaptureStderr() {
        return captureStderr;
    }

    public void setCaptureStderr(Boolean captureStderr) {
        this.captureStderr = captureStderr;
    }

    public Boolean getCaptureGitDiff() {
        return captureGitDiff;
    }

    public void setCaptureGitDiff(Boolean captureGitDiff) {
        this.captureGitDiff = captureGitDiff;
    }

    public Long getOutputSchemaId() {
        return outputSchemaId;
    }

    public void setOutputSchemaId(Long outputSchemaId) {
        this.outputSchemaId = outputSchemaId;
    }
}
