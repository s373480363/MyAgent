package com.myagent.runtime.executor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myagent.common.error.BizException;
import com.myagent.common.error.ErrorCode;
import com.myagent.run.domain.TraceEventType;
import com.myagent.runtime.NodeExecutionContext;
import com.myagent.runtime.NodeExecutionResult;
import com.myagent.runtime.NodeExecutor;
import com.myagent.runtime.SupportsNodeType;
import com.myagent.runtime.TraceEventRecord;
import com.myagent.schema.validation.ValidationStage;
import com.myagent.tool.runtime.ToolDescriptor;
import com.myagent.tool.runtime.ToolExecutionRequest;
import com.myagent.tool.runtime.ToolRegistry;
import com.myagent.workflow.domain.WorkflowNodeType;
import org.springframework.stereotype.Component;

/**
 * TOOL 节点执行器。
 */
@Component
public class ToolNodeExecutor extends AbstractNodeExecutorSupport implements NodeExecutor, SupportsNodeType {

    /**
     * 工具注册目录。
     */
    private final ToolRegistry toolRegistry;

    /**
     * 构造 TOOL 节点执行器。
     *
     * @param objectMapper JSON 对象映射器
     * @param toolRegistry 工具注册目录
     */
    public ToolNodeExecutor(ObjectMapper objectMapper, ToolRegistry toolRegistry) {
        super(objectMapper);
        this.toolRegistry = toolRegistry;
    }

    /**
     * 返回支持的节点类型。
     *
     * @return 节点类型
     */
    @Override
    public WorkflowNodeType supportedNodeType() {
        return WorkflowNodeType.TOOL;
    }

    /**
     * 执行 TOOL 节点。
     *
     * @param context 节点执行上下文
     * @return 节点执行结果
     */
    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        long startedAt = System.nanoTime();
        JsonNode input = extractInput(context);
        validateSchema(context, input, context.nodeDefinition().getInputSchemaRef(), ValidationStage.NODE_INPUT);
        String toolKey = null;
        String executorType = null;
        JsonNode output = null;
        try {
            toolKey = readRequiredConfigText(context.nodeDefinition().getConfig(), "toolKey", "工具节点缺少 toolKey。");
            ToolDescriptor descriptor = toolRegistry.getEnabledTool(toolKey);
            executorType = descriptor.record().executorType();
            output = descriptor.executor().execute(new ToolExecutionRequest(descriptor.record(), input));
            validateOutputSchema(context, output, context.nodeDefinition().getOutputSchemaRef(), ValidationStage.NODE_OUTPUT);
            long durationMs = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
            writeToolTrace(context, toolKey, executorType, input, output, durationMs, null);
            return NodeExecutionResult.success(output, durationMs);
        } catch (BizException exception) {
            writeToolTrace(
                    context,
                    toolKey,
                    executorType,
                    input,
                    output,
                    java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt),
                    exception.getMessage()
            );
            throw exception;
        }
    }

    /**
     * 写入工具调用 Trace。
     *
     * @param context 节点执行上下文
     * @param toolKey 工具标识
     * @param executorType 执行器类型
     * @param input 输入 JSON
     * @param output 输出 JSON
     * @param durationMs 耗时毫秒
     * @param error 错误消息
     */
    private void writeToolTrace(
            NodeExecutionContext context,
            String toolKey,
            String executorType,
            JsonNode input,
            JsonNode output,
            long durationMs,
            String error
    ) {
        com.fasterxml.jackson.databind.node.ObjectNode detail = objectMapper.createObjectNode()
                .put("toolKey", toolKey)
                .put("executorType", executorType)
                .put("durationMs", durationMs)
                .put("error", error);
        detail.set("input", input);
        detail.set("output", output);
        context.traceWriter().writeEvent(new TraceEventRecord(
                context.agentRunDbId(),
                context.nodeRunDbId(),
                null,
                TraceEventType.TOOL_CALL,
                error == null ? "工具节点执行完成：" + toolKey : "工具节点执行失败：" + toolKey,
                detail
        ));
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
        throw new BizException(ErrorCode.TOOL_CALL_FAILED, message);
    }
}
