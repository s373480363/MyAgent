package com.myagent.runtime.executor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myagent.common.domain.EnableStatus;
import com.myagent.common.error.BizException;
import com.myagent.common.error.ErrorCode;
import com.myagent.run.domain.TraceEventType;
import com.myagent.runtime.NodeExecutionContext;
import com.myagent.runtime.NodeExecutionResult;
import com.myagent.runtime.NodeExecutor;
import com.myagent.runtime.SupportsNodeType;
import com.myagent.runtime.TraceEventRecord;
import com.myagent.schema.validation.ValidationStage;
import com.myagent.tool.repository.ToolRecord;
import com.myagent.tool.repository.ToolRepository;
import com.myagent.workflow.domain.WorkflowNodeType;
import org.springframework.stereotype.Component;

/**
 * TOOL 节点执行器。
 */
@Component
public class ToolNodeExecutor extends AbstractNodeExecutorSupport implements NodeExecutor, SupportsNodeType {

    /**
     * 工具仓储。
     */
    private final ToolRepository toolRepository;

    /**
     * 构造 TOOL 节点执行器。
     *
     * @param objectMapper JSON 对象映射器
     * @param toolRepository 工具仓储
     */
    public ToolNodeExecutor(ObjectMapper objectMapper, ToolRepository toolRepository) {
        super(objectMapper);
        this.toolRepository = toolRepository;
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
        String toolKey = readRequiredConfigText(context.nodeDefinition().getConfig(), "toolKey", "工具节点缺少 toolKey。");
        ToolRecord tool = toolRepository.findByToolKey(toolKey)
                .orElseThrow(() -> new BizException(ErrorCode.TOOL_CALL_FAILED, "工具不存在：" + toolKey));
        if (tool.status() != EnableStatus.ENABLED) {
            throw new BizException(ErrorCode.TOOL_CALL_FAILED, "工具已停用：" + toolKey);
        }
        JsonNode output = executeRegisteredTool(tool, input);
        validateSchema(context, output, context.nodeDefinition().getOutputSchemaRef(), ValidationStage.NODE_OUTPUT);
        context.traceWriter().writeEvent(new TraceEventRecord(
                context.agentRunDbId(),
                context.nodeRunDbId(),
                null,
                TraceEventType.TOOL_CALL,
                "工具节点执行完成：" + toolKey,
                objectMapper.createObjectNode()
                        .put("toolKey", toolKey)
                        .put("executorType", tool.executorType())
                        .set("output", output)
        ));
        return NodeExecutionResult.success(output, java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt));
    }

    /**
     * 执行已注册工具。
     *
     * @param tool 工具记录
     * @param input 节点输入
     * @return 工具输出
     */
    private JsonNode executeRegisteredTool(ToolRecord tool, JsonNode input) {
        String executorType = tool.executorType() == null ? "" : tool.executorType();
        JsonNode config = tool.executorConfigJson();
        return switch (executorType) {
            case "ECHO" -> input;
            case "STATIC_JSON" -> config != null && config.has("output") ? config.get("output") : objectMapper.createObjectNode();
            default -> throw new BizException(ErrorCode.TOOL_CALL_FAILED, "工具执行器未注册：" + executorType);
        };
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
