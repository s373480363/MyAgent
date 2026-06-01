package com.myagent.runtime.executor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myagent.model.ModelInvocationRequest;
import com.myagent.model.ModelInvocationResult;
import com.myagent.model.OpenAiModelGateway;
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
 * LLM 节点执行器。
 */
@Component
public class LlmNodeExecutor extends AbstractNodeExecutorSupport implements NodeExecutor, SupportsNodeType {

    /**
     * 模型网关。
     */
    private final OpenAiModelGateway modelGateway;

    /**
     * 构造 LLM 节点执行器。
     *
     * @param objectMapper JSON 对象映射器
     * @param modelGateway 模型网关
     */
    public LlmNodeExecutor(ObjectMapper objectMapper, OpenAiModelGateway modelGateway) {
        super(objectMapper);
        this.modelGateway = modelGateway;
    }

    /**
     * 返回支持的节点类型。
     *
     * @return 节点类型
     */
    @Override
    public WorkflowNodeType supportedNodeType() {
        return WorkflowNodeType.LLM;
    }

    /**
     * 执行 LLM 节点。
     *
     * @param context 节点执行上下文
     * @return 节点执行结果
     */
    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        JsonNode input = extractInput(context);
        validateSchema(context, input, context.nodeDefinition().getInputSchemaRef(), ValidationStage.NODE_INPUT);
        JsonNode config = context.nodeDefinition().getConfig();
        boolean structuredOutput = context.nodeDefinition().getOutputSchemaRef() != null;
        ModelInvocationRequest request = new ModelInvocationRequest(
                readText(config, "model", context.agentDefinition().defaultModel()),
                renderSystemPromptTemplate(context, input),
                renderUserPromptTemplate(context, input),
                input,
                readDecimal(config, "temperature", context.agentDefinition().temperature()),
                structuredOutput
        );
        context.traceWriter().writeEvent(new TraceEventRecord(
                context.agentRunDbId(),
                context.nodeRunDbId(),
                null,
                TraceEventType.MODEL_REQUEST,
                "调用模型：" + request.model(),
                objectMapper.valueToTree(request)
        ));
        ModelInvocationResult result = modelGateway.invoke(request);
        context.traceWriter().writeEvent(new TraceEventRecord(
                context.agentRunDbId(),
                context.nodeRunDbId(),
                null,
                TraceEventType.MODEL_RESPONSE,
                "模型调用完成。",
                objectMapper.valueToTree(result)
        ));
        validateOutputSchema(context, result.output(), context.nodeDefinition().getOutputSchemaRef(), ValidationStage.NODE_OUTPUT);
        return NodeExecutionResult.success(result.output(), result.durationMs());
    }
}
