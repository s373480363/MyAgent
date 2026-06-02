package com.myagent.externalagent.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.myagent.externalagent.application.result.ExternalAgentTestResult;
import com.myagent.externalagent.domain.ExternalAgentType;
import com.myagent.externalagent.repository.ExternalAgentRecord;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 外部 Agent 测试执行器。
 */
@Component
public class ExternalAgentTestExecutor {

    /**
     * JSON 对象映射器。
     */
    private final ObjectMapper objectMapper;

    /**
     * commandJson 编解码工具。
     */
    private final ExternalAgentCommandJsonCodec commandJsonCodec;

    /**
     * 构造测试执行器。
     *
     * @param objectMapper JSON 对象映射器
     * @param commandJsonCodec commandJson 编解码工具
     */
    public ExternalAgentTestExecutor(
            ObjectMapper objectMapper,
            ExternalAgentCommandJsonCodec commandJsonCodec
    ) {
        this.objectMapper = objectMapper;
        this.commandJsonCodec = commandJsonCodec;
    }

    /**
     * 执行外部 Agent 测试。
     *
     * @param record 外部 Agent 记录
     * @param prompt 测试提示词
     * @param input 输入 JSON
     * @return 测试结果
     */
    public ExternalAgentTestResult execute(ExternalAgentRecord record, String prompt, JsonNode input) {
        Instant startedAt = Instant.now();
        return switch (record.adapterType()) {
            case CUSTOM_HTTP -> testHttp(record, prompt, input, startedAt);
            case CODEX_CLI, OPENCODE_CLI, CUSTOM_CLI -> testCli(record, prompt, input, startedAt);
        };
    }

    /**
     * 测试 CLI 外部 Agent。
     *
     * @param record 外部 Agent 记录
     * @param prompt 测试提示词
     * @param input 输入 JSON
     * @param startedAt 开始时间
     * @return 测试结果
     */
    private ExternalAgentTestResult testCli(
            ExternalAgentRecord record,
            String prompt,
            JsonNode input,
            Instant startedAt
    ) {
        try {
            List<String> command = new ArrayList<>();
            command.add(commandJsonCodec.getCliCommand(record.commandJson()));
            for (String argument : commandJsonCodec.getCliArguments(record.commandJson())) {
                command.add(renderStringTemplate(argument, prompt, input));
            }

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            if (record.workingDirectory() != null && !record.workingDirectory().isBlank()) {
                processBuilder.directory(new java.io.File(record.workingDirectory()));
            }
            processBuilder.environment().putAll(commandJsonCodec.getCliEnvironment(record.commandJson()));

            Process process = processBuilder.start();
            boolean finished = process.waitFor(record.timeoutSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return failedResult("TIMEOUT", null, null, "外部 Agent 执行超时。", startedAt);
            }

            String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.exitValue();
            ParsedOutput parsedOutput = parseOutput(commandJsonCodec.getResultSourceType(record.commandJson()), stdout, null);

            boolean success = exitCode == 0 && parsedOutput.errorMessage() == null;
            String errorMessage = parsedOutput.errorMessage();
            if (errorMessage == null && exitCode != 0) {
                errorMessage = "外部 Agent 进程退出码为 " + exitCode + "。";
            }
            return new ExternalAgentTestResult(
                    success,
                    success ? "SUCCESS" : "FAILED",
                    exitCode,
                    null,
                    stdout,
                    stderr,
                    parsedOutput.outputJson(),
                    parsedOutput.summary(),
                    errorMessage,
                    Duration.between(startedAt, Instant.now()).toMillis()
            );
        } catch (IOException exception) {
            return failedResult("FAILED", null, null, "执行外部 Agent 进程失败：" + exception.getMessage(), startedAt);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return failedResult("FAILED", null, null, "测试执行被中断。", startedAt);
        }
    }

    /**
     * 测试 HTTP 外部 Agent。
     *
     * @param record 外部 Agent 记录
     * @param prompt 测试提示词
     * @param input 输入 JSON
     * @param startedAt 开始时间
     * @return 测试结果
     */
    private ExternalAgentTestResult testHttp(
            ExternalAgentRecord record,
            String prompt,
            JsonNode input,
            Instant startedAt
    ) {
        java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(record.timeoutSeconds()))
                .build();
        try {
            JsonNode requestBodyNode = renderJsonTemplate(commandJsonCodec.getHttpBodyTemplate(record.commandJson()), prompt, input);
            String requestBody = objectMapper.writeValueAsString(requestBodyNode);

            java.net.http.HttpRequest.Builder requestBuilder = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(commandJsonCodec.getHttpUrl(record.commandJson())))
                    .timeout(Duration.ofSeconds(record.timeoutSeconds()));

            for (Map.Entry<String, String> header : commandJsonCodec.mergeHttpHeaders(record.commandJson()).entrySet()) {
                requestBuilder.header(header.getKey(), header.getValue());
            }
            requestBuilder.method(
                    commandJsonCodec.getHttpMethod(record.commandJson()),
                    java.net.http.HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8)
            );

            java.net.http.HttpResponse<String> response = client.send(
                    requestBuilder.build(),
                    java.net.http.HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            ParsedOutput parsedOutput = parseOutput(
                    commandJsonCodec.getResultSourceType(record.commandJson()),
                    null,
                    response.body()
            );
            boolean success = response.statusCode() >= 200 && response.statusCode() < 300 && parsedOutput.errorMessage() == null;
            String errorMessage = parsedOutput.errorMessage();
            if (errorMessage == null && !success) {
                errorMessage = "外部 HTTP Agent 返回状态码 " + response.statusCode() + "。";
            }
            return new ExternalAgentTestResult(
                    success,
                    success ? "SUCCESS" : "FAILED",
                    null,
                    response.statusCode(),
                    null,
                    null,
                    parsedOutput.outputJson(),
                    parsedOutput.summary(),
                    errorMessage,
                    Duration.between(startedAt, Instant.now()).toMillis()
            );
        } catch (IOException exception) {
            return failedResult("FAILED", null, null, "调用外部 HTTP Agent 失败：" + exception.getMessage(), startedAt);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return failedResult("FAILED", null, null, "测试执行被中断。", startedAt);
        }
    }

    /**
     * 解析业务输出。
     *
     * @param resultSourceType resultSource 类型
     * @param stdout stdout
     * @param httpBody HTTP 响应体
     * @return 解析结果
     */
    private ParsedOutput parseOutput(String resultSourceType, String stdout, String httpBody) {
        String raw = stdout != null ? stdout : httpBody;
        try {
            return switch (resultSourceType) {
                case "STDOUT_JSON" -> parseJsonOutput(stdout);
                case "STDOUT_JSON_LAST_MESSAGE" -> parseLastLineJsonOutput(stdout);
                case "HTTP_BODY_JSON" -> parseJsonOutput(httpBody);
                default -> parseTextOutput(raw);
            };
        } catch (Exception exception) {
            return new ParsedOutput(null, truncate(raw), null);
        }
    }

    /**
     * 解析 JSON 输出。
     *
     * @param raw 原始文本
     * @return 解析结果
     * @throws IOException JSON 解析失败时抛出
     */
    private ParsedOutput parseJsonOutput(String raw) throws IOException {
        if (raw == null || raw.isBlank()) {
            return new ParsedOutput(null, "", null);
        }
        JsonNode jsonNode = objectMapper.readTree(raw);
        return new ParsedOutput(jsonNode, truncate(raw), null);
    }

    /**
     * 解析最后一行 JSON 输出。
     *
     * @param raw 原始文本
     * @return 解析结果
     * @throws IOException JSON 解析失败时抛出
     */
    private ParsedOutput parseLastLineJsonOutput(String raw) throws IOException {
        if (raw == null || raw.isBlank()) {
            return new ParsedOutput(null, "", null);
        }
        String[] lines = raw.split("\\R");
        for (int index = lines.length - 1; index >= 0; index--) {
            if (!lines[index].isBlank()) {
                JsonNode jsonNode = objectMapper.readTree(lines[index]);
                return new ParsedOutput(jsonNode, truncate(lines[index]), null);
            }
        }
        return new ParsedOutput(null, "", null);
    }

    /**
     * 解析文本输出。
     *
     * @param raw 原始文本
     * @return 解析结果
     */
    private ParsedOutput parseTextOutput(String raw) {
        return new ParsedOutput(null, truncate(raw), null);
    }

    /**
     * 渲染字符串模板。
     *
     * @param template 原始模板
     * @param prompt 提示词
     * @param input 输入 JSON
     * @return 渲染后的文本
     */
    private String renderStringTemplate(String template, String prompt, JsonNode input) {
        String result = template == null ? "" : template;
        result = result.replace("{prompt}", prompt == null ? "" : prompt);
        result = result.replace("{inputJson}", input == null ? "{}" : input.toString());
        return result;
    }

    /**
     * 渲染 JSON 模板。
     *
     * @param template 原始 JSON 模板
     * @param prompt 提示词
     * @param input 输入 JSON
     * @return 渲染后的 JSON
     */
    private JsonNode renderJsonTemplate(JsonNode template, String prompt, JsonNode input) {
        if (template == null || template.isNull()) {
            return objectMapper.createObjectNode();
        }
        if (template.isTextual()) {
            String value = template.asText();
            if ("{inputJson}".equals(value)) {
                return input == null ? objectMapper.createObjectNode() : input.deepCopy();
            }
            if ("{prompt}".equals(value)) {
                return TextNode.valueOf(prompt == null ? "" : prompt);
            }
            return TextNode.valueOf(renderStringTemplate(value, prompt, input));
        }
        if (template.isObject()) {
            com.fasterxml.jackson.databind.node.ObjectNode result = objectMapper.createObjectNode();
            template.properties().forEach(entry -> result.set(entry.getKey(), renderJsonTemplate(entry.getValue(), prompt, input)));
            return result;
        }
        if (template.isArray()) {
            com.fasterxml.jackson.databind.node.ArrayNode result = objectMapper.createArrayNode();
            template.forEach(item -> result.add(renderJsonTemplate(item, prompt, input)));
            return result;
        }
        return template.deepCopy();
    }

    /**
     * 构造失败结果。
     *
     * @param status 状态
     * @param exitCode 退出码
     * @param httpStatus HTTP 状态码
     * @param errorMessage 错误消息
     * @param startedAt 开始时间
     * @return 失败结果
     */
    private ExternalAgentTestResult failedResult(
            String status,
            Integer exitCode,
            Integer httpStatus,
            String errorMessage,
            Instant startedAt
    ) {
        return new ExternalAgentTestResult(
                false,
                status,
                exitCode,
                httpStatus,
                null,
                null,
                null,
                null,
                errorMessage,
                Duration.between(startedAt, Instant.now()).toMillis()
        );
    }

    /**
     * 截断文本摘要。
     *
     * @param raw 原始文本
     * @return 截断后的摘要
     */
    private String truncate(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.strip();
        if (normalized.length() <= 1000) {
            return normalized;
        }
        return normalized.substring(0, 1000);
    }

    /**
     * 解析结果载体。
     *
     * @param outputJson 业务输出 JSON
     * @param summary 输出摘要
     * @param errorMessage 错误消息
     */
    private record ParsedOutput(JsonNode outputJson, String summary, String errorMessage) {
    }
}
