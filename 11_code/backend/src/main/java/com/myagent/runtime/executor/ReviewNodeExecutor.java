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
 * REVIEW 节点执行器。
 */
@Component
public class ReviewNodeExecutor extends AbstractNodeExecutorSupport implements NodeExecutor, SupportsNodeType {

    /**
     * 模型网关。
     */
    private final OpenAiModelGateway modelGateway;

    /**
     * 构造 REVIEW 节点执行器。
     *
     * @param objectMapper JSON 对象映射器
     * @param modelGateway 模型网关
     */
    public ReviewNodeExecutor(ObjectMapper objectMapper, OpenAiModelGateway modelGateway) {
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
        return WorkflowNodeType.REVIEW;
    }

    /**
     * 执行 REVIEW 节点。
     *
     * @param context 节点执行上下文
     * @return 节点执行结果
     */
    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        long startedAt = System.nanoTime();
        JsonNode input = extractInput(context);
        JsonNode config = context.nodeDefinition().getConfig();
        ModelInvocationResult result = modelGateway.invoke(new ModelInvocationRequest(
                context.agentDefinition().defaultModel(),
                context.agentDefinition().systemPrompt(),
                config != null && config.hasNonNull("prompt") ? config.get("prompt").asText() : input.toString(),
                input,
                context.agentDefinition().temperature(),
                context.nodeDefinition().getOutputSchemaRef() != null
        ));
        validateSchema(context, result.output(), context.nodeDefinition().getOutputSchemaRef(), ValidationStage.NODE_OUTPUT);
        context.traceWriter().writeEvent(new TraceEventRecord(
                context.agentRunDbId(),
                context.nodeRunDbId(),
                null,
                TraceEventType.MODEL_RESPONSE,
                "审核节点模型输出完成。",
                objectMapper.valueToTree(result)
        ));
        return NodeExecutionResult.success(result.output(), java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt));
    }
}
