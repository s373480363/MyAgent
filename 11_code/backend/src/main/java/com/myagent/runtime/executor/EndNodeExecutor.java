package com.myagent.runtime.executor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myagent.runtime.NodeExecutionContext;
import com.myagent.runtime.NodeExecutionResult;
import com.myagent.runtime.NodeExecutor;
import com.myagent.runtime.SupportsNodeType;
import com.myagent.schema.validation.ValidationStage;
import com.myagent.workflow.domain.WorkflowNodeType;
import org.springframework.stereotype.Component;

/**
 * END 节点执行器。
 */
@Component
public class EndNodeExecutor extends AbstractNodeExecutorSupport implements NodeExecutor, SupportsNodeType {

    /**
     * 构造 END 节点执行器。
     *
     * @param objectMapper JSON 对象映射器
     */
    public EndNodeExecutor(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    /**
     * 返回支持的节点类型。
     *
     * @return 节点类型
     */
    @Override
    public WorkflowNodeType supportedNodeType() {
        return WorkflowNodeType.END;
    }

    /**
     * 执行 END 节点。
     *
     * @param context 节点执行上下文
     * @return 节点执行结果
     */
    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        long startedAt = System.nanoTime();
        JsonNode output = context.nodeDefinition().getInputMapping() == null
                ? context.workflowContext().root().path("output")
                : extractInput(context);
        if (output.isMissingNode() || output.isNull()) {
            output = context.workflowContext().root();
        }
        validateSchema(context, output, context.nodeDefinition().getOutputSchemaRef(), ValidationStage.END_OUTPUT);
        return NodeExecutionResult.success(output, elapsedMillis(startedAt));
    }

    /**
     * 计算耗时。
     *
     * @param startedAtNanos 开始纳秒
     * @return 耗时毫秒
     */
    private long elapsedMillis(long startedAtNanos) {
        return java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNanos);
    }
}
