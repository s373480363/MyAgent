package com.myagent.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myagent.common.error.BizException;
import com.myagent.common.error.ErrorCode;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 默认 OpenAI 模型网关。
 */
@Component
public class DefaultOpenAiModelGateway implements OpenAiModelGateway {

    /**
     * JSON 对象映射器。
     */
    private final ObjectMapper objectMapper;

    /**
     * OpenAI ChatModel 延迟提供器。
     */
    private final ObjectProvider<OpenAiChatModel> chatModelProvider;

    /**
     * 构造模型网关。
     *
     * @param objectMapper JSON 对象映射器
     * @param chatModelProvider OpenAI ChatModel 延迟提供器
     */
    public DefaultOpenAiModelGateway(
            ObjectMapper objectMapper,
            ObjectProvider<OpenAiChatModel> chatModelProvider
    ) {
        this.objectMapper = objectMapper;
        this.chatModelProvider = chatModelProvider;
    }

    /**
     * 调用模型。
     *
     * @param request 模型调用请求
     * @return 模型调用结果
     */
    @Override
    public ModelInvocationResult invoke(ModelInvocationRequest request) {
        long startedAt = System.nanoTime();
        OpenAiChatModel chatModel = chatModelProvider.getIfAvailable();
        if (chatModel == null) {
            throw new BizException(ErrorCode.NODE_EXECUTION_FAILED, "OpenAI 模型客户端未配置。");
        }
        try {
            ChatResponse response = chatModel.call(toPrompt(request));
            String rawText = response.getResult().getOutput().getText();
            return new ModelInvocationResult(
                    parseOutput(rawText, request.structuredOutput()),
                    rawText,
                    elapsedMillis(startedAt)
            );
        } catch (BizException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BizException(ErrorCode.NODE_EXECUTION_FAILED, "模型调用失败：" + exception.getMessage());
        }
    }

    /**
     * 构造 Spring AI Prompt。
     *
     * @param request 模型调用请求
     * @return Prompt
     */
    private Prompt toPrompt(ModelInvocationRequest request) {
        List<Message> messages = new ArrayList<>();
        if (request.systemPrompt() != null && !request.systemPrompt().isBlank()) {
            messages.add(new SystemMessage(request.systemPrompt()));
        }
        messages.add(new UserMessage(request.userPrompt() == null ? "" : request.userPrompt()));
        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
                .model(request.model());
        if (request.temperature() != null) {
            optionsBuilder.temperature(request.temperature().doubleValue());
        }
        if (request.structuredOutput()) {
            optionsBuilder.responseFormat(ResponseFormat.builder().type(ResponseFormat.Type.JSON_OBJECT).build());
        }
        return new Prompt(messages, optionsBuilder.build());
    }

    /**
     * 解析模型输出。
     *
     * @param rawText 模型原始文本
     * @param structuredOutput 是否结构化输出
     * @return 业务输出 JSON
     */
    private com.fasterxml.jackson.databind.JsonNode parseOutput(String rawText, boolean structuredOutput) {
        if (!structuredOutput) {
            return objectMapper.getNodeFactory().textNode(rawText == null ? "" : rawText);
        }
        try {
            return objectMapper.readTree(rawText == null ? "" : rawText);
        } catch (Exception exception) {
            throw new BizException(ErrorCode.NODE_EXECUTION_FAILED, "模型结构化输出解析失败：" + exception.getMessage());
        }
    }

    /**
     * 计算耗时毫秒。
     *
     * @param startedAtNanos 开始纳秒
     * @return 耗时毫秒
     */
    private long elapsedMillis(long startedAtNanos) {
        return java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNanos);
    }
}
