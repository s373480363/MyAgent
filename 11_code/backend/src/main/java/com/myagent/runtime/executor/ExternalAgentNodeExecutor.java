package com.myagent.runtime.executor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.myagent.common.domain.EnableStatus;
import com.myagent.common.error.BizException;
import com.myagent.common.error.ErrorCode;
import com.myagent.externalagent.application.ExternalAgentCommandJsonCodec;
import com.myagent.externalagent.application.ExternalAgentTestExecutor;
import com.myagent.externalagent.application.result.ExternalAgentTestResult;
import com.myagent.externalagent.repository.ExternalAgentRecord;
import com.myagent.externalagent.repository.ExternalAgentRepository;
import com.myagent.run.domain.TraceEventType;
import com.myagent.runtime.NodeExecutionContext;
import com.myagent.runtime.NodeExecutionResult;
import com.myagent.runtime.NodeExecutor;
import com.myagent.runtime.SupportsNodeType;
import com.myagent.runtime.TraceEventRecord;
import com.myagent.schema.validation.ValidationStage;
import com.myagent.workflow.domain.WorkflowNodeType;
import org.springframework.stereotype.Component;

/**
 * EXTERNAL_AGENT 节点执行器。
 */
@Component
public class ExternalAgentNodeExecutor extends AbstractNodeExecutorSupport implements NodeExecutor, SupportsNodeType {

    /**
     * 外部 Agent 仓储。
     */
    private final ExternalAgentRepository externalAgentRepository;

    /**
     * 外部 Agent 命令配置编解码器。
     */
    private final ExternalAgentCommandJsonCodec commandJsonCodec;

    /**
     * 外部 Agent 统一执行器。
     */
    private final ExternalAgentTestExecutor externalAgentTestExecutor;

    /**
     * 构造 EXTERNAL_AGENT 节点执行器。
     *
     * @param objectMapper JSON 对象映射器
     * @param externalAgentRepository 外部 Agent 仓储
     * @param commandJsonCodec 外部 Agent 命令配置编解码器
     * @param externalAgentTestExecutor 外部 Agent 统一执行器
     */
    public ExternalAgentNodeExecutor(
            ObjectMapper objectMapper,
            ExternalAgentRepository externalAgentRepository,
            ExternalAgentCommandJsonCodec commandJsonCodec,
            ExternalAgentTestExecutor externalAgentTestExecutor
    ) {
        super(objectMapper);
        this.externalAgentRepository = externalAgentRepository;
        this.commandJsonCodec = commandJsonCodec;
        this.externalAgentTestExecutor = externalAgentTestExecutor;
    }

    /**
     * 返回支持的节点类型。
     *
     * @return 节点类型
     */
    @Override
    public WorkflowNodeType supportedNodeType() {
        return WorkflowNodeType.EXTERNAL_AGENT;
    }

    /**
     * 执行 EXTERNAL_AGENT 节点。
     *
     * @param context 节点执行上下文
     * @return 节点执行结果
     */
    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        long startedAt = System.nanoTime();
        JsonNode input = extractInput(context);
        String adapterKey = readRequiredConfigText(context.nodeDefinition().getConfig(), "adapterKey", "外部 Agent 节点缺少 adapterKey。");
        ExternalAgentRecord adapter = externalAgentRepository.findByAdapterKey(adapterKey)
                .orElseThrow(() -> new BizException(ErrorCode.EXTERNAL_AGENT_CALL_FAILED, "外部 Agent 适配器不存在：" + adapterKey));
        if (adapter.status() != EnableStatus.ENABLED) {
            throw new BizException(ErrorCode.EXTERNAL_AGENT_CALL_FAILED, "外部 Agent 适配器已停用：" + adapterKey);
        }
        commandJsonCodec.assertSecretsConfigured(adapter.adapterType(), adapter.commandJson());

        String prompt = renderPrompt(context.nodeDefinition().getConfig(), input);
        ExternalAgentRecord runtimeAdapter = withRuntimeTimeout(adapter, context.nodeDefinition().getTimeoutSeconds());
        ExternalAgentTestResult result = externalAgentTestExecutor.execute(runtimeAdapter, prompt, input);
        JsonNode output = result.outputJson() == null || result.outputJson().isNull()
                ? TextNode.valueOf(result.summary() == null ? "" : result.summary())
                : result.outputJson();
        writeTrace(context, adapterKey, runtimeAdapter, result, output);
        if (!result.success()) {
            throw new BizException(ErrorCode.EXTERNAL_AGENT_CALL_FAILED, result.errorMessage() == null
                    ? "外部 Agent 调用失败：" + adapterKey
                    : result.errorMessage());
        }
        validateSchema(context, output, context.nodeDefinition().getOutputSchemaRef(), ValidationStage.NODE_OUTPUT);
        return NodeExecutionResult.success(output, java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt));
    }

    /**
     * 写入外部 Agent 调用 Trace。
     *
     * @param context 节点执行上下文
     * @param adapterKey 外部 Agent 标识
     * @param result 执行结果
     * @param output 业务输出
     */
    private void writeTrace(
            NodeExecutionContext context,
            String adapterKey,
            ExternalAgentRecord adapter,
            ExternalAgentTestResult result,
            JsonNode output
    ) {
        ObjectNode detail = objectMapper.createObjectNode()
                .put("adapterKey", adapterKey)
                .put("success", result.success())
                .put("status", result.status())
                .put("durationMs", result.durationMs());
        if (result.exitCode() != null) {
            detail.put("exitCode", result.exitCode());
        }
        if (result.httpStatus() != null) {
            detail.put("httpStatus", result.httpStatus());
        }
        if (result.summary() != null) {
            detail.put("summary", result.summary());
        }
        if (result.errorMessage() != null) {
            detail.put("errorMessage", result.errorMessage());
        }
        if (adapter.captureStdout() && result.stdout() != null) {
            detail.put("stdout", result.stdout());
        }
        if (adapter.captureStderr() && result.stderr() != null) {
            detail.put("stderr", result.stderr());
        }
        detail.set("output", output);
        context.traceWriter().writeEvent(new TraceEventRecord(
                context.agentRunDbId(),
                context.nodeRunDbId(),
                null,
                TraceEventType.EXTERNAL_AGENT_CALL,
                result.success() ? "外部 Agent 节点执行完成：" + adapterKey : "外部 Agent 节点执行失败：" + adapterKey,
                detail
        ));
    }

    /**
     * 按节点级超时生成运行期适配器记录。
     *
     * @param adapter 持久化适配器
     * @param nodeTimeoutSeconds 节点级超时
     * @return 运行期适配器
     */
    private ExternalAgentRecord withRuntimeTimeout(ExternalAgentRecord adapter, Integer nodeTimeoutSeconds) {
        int timeoutSeconds = nodeTimeoutSeconds == null ? adapter.timeoutSeconds() : nodeTimeoutSeconds;
        return new ExternalAgentRecord(
                adapter.id(),
                adapter.adapterKey(),
                adapter.adapterType(),
                adapter.name(),
                adapter.description(),
                adapter.commandJson(),
                adapter.workingDirectory(),
                timeoutSeconds,
                adapter.captureStdout(),
                adapter.captureStderr(),
                adapter.captureGitDiff(),
                adapter.outputSchemaId(),
                adapter.status(),
                adapter.createdAt(),
                adapter.updatedAt()
        );
    }

    /**
     * 渲染节点提示词模板。
     *
     * @param config 节点配置
     * @param input 节点输入
     * @return 渲染后的提示词
     */
    private String renderPrompt(JsonNode config, JsonNode input) {
        String template = null;
        if (config != null && config.hasNonNull("promptTemplate") && config.get("promptTemplate").isTextual()) {
            template = config.get("promptTemplate").asText();
        } else if (config != null && config.hasNonNull("prompt") && config.get("prompt").isTextual()) {
            template = config.get("prompt").asText();
        }
        String raw = template == null || template.isBlank() ? input.toString() : template;
        return raw.replace("{inputJson}", input.toString());
    }

    /**
     * 读取必填文本配置。
     *
     * @param config 节点配置
     * @param fieldName 字段名
     * @param message 错误消息
     * @return 文本值
     */
    private String readRequiredConfigText(JsonNode config, String fieldName, String message) {
        if (config != null && config.hasNonNull(fieldName) && config.get(fieldName).isTextual()
                && !config.get(fieldName).asText().isBlank()) {
            return config.get(fieldName).asText();
        }
        throw new BizException(ErrorCode.EXTERNAL_AGENT_CALL_FAILED, message);
    }
}
