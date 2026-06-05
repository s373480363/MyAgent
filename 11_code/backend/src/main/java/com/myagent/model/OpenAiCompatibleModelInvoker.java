package com.myagent.model;

import com.fasterxml.jackson.databind.JsonNode;
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
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * OpenAI-compatible 模型调用器。
 */
@Component
public class OpenAiCompatibleModelInvoker {

    /**
     * JSON 对象映射器。
     */
    private final ObjectMapper objectMapper;

    /**
     * 构造调用器。
     *
     * @param objectMapper JSON 对象映射器
     */
    public OpenAiCompatibleModelInvoker(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 按解析后的供应商路由调用模型。
     *
     * @param route 已解析路由
     * @param request 模型调用请求
     * @return 模型调用结果
     */
    public ModelInvocationResult invoke(ResolvedModelRoute route, ModelInvocationRequest request) {
        long startedAt = System.nanoTime();
        try {
            ChatResponse response = createChatModel(route, request).call(toPrompt(route, request));
            String rawText = response.getResult().getOutput().getText();
            return new ModelInvocationResult(
                    parseOutput(rawText, request.structuredOutput()),
                    rawText,
                    elapsedMillis(startedAt)
            );
        } catch (BizException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BizException(ErrorCode.NODE_EXECUTION_FAILED, "模型调用失败，请检查模型供应商配置、模型名称或网络连接。");
        }
    }

    /**
     * 构造 Spring AI ChatModel。
     *
     * @param route 已解析路由
     * @param request 模型调用请求
     * @return ChatModel
     */
    private OpenAiChatModel createChatModel(ResolvedModelRoute route, ModelInvocationRequest request) {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(route.baseUrl())
                .apiKey(route.decryptedApiKey())
                .build();
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(defaultOptions(route, request))
                .build();
    }

    /**
     * 构造默认 OpenAI 选项。
     *
     * @param route 已解析路由
     * @param request 模型调用请求
     * @return 默认选项
     */
    private OpenAiChatOptions defaultOptions(ResolvedModelRoute route, ModelInvocationRequest request) {
        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
                .model(route.upstreamModelName());
        BigDecimal temperature = request.temperature() == null ? route.defaultTemperature() : request.temperature();
        if (temperature != null) {
            optionsBuilder.temperature(temperature.doubleValue());
        }
        if (request.structuredOutput()) {
            optionsBuilder.responseFormat(ResponseFormat.builder().type(ResponseFormat.Type.JSON_OBJECT).build());
        }
        return optionsBuilder.build();
    }

    /**
     * 构造 Prompt。
     *
     * @param route 已解析路由
     * @param request 模型调用请求
     * @return Prompt
     */
    private Prompt toPrompt(ResolvedModelRoute route, ModelInvocationRequest request) {
        List<Message> messages = new ArrayList<>();
        if (request.systemPrompt() != null && !request.systemPrompt().isBlank()) {
            messages.add(new SystemMessage(request.systemPrompt()));
        }
        messages.add(new UserMessage(request.userPrompt() == null ? "" : request.userPrompt()));
        return new Prompt(messages, defaultOptions(route, request));
    }

    /**
     * 解析模型输出。
     *
     * @param rawText 原始文本
     * @param structuredOutput 是否结构化输出
     * @return 业务输出 JSON
     */
    private JsonNode parseOutput(String rawText, boolean structuredOutput) {
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
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNanos);
    }
}
