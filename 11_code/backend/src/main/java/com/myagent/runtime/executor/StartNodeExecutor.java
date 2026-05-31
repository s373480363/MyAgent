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
 * START 节点执行器。
 */
@Component
public class StartNodeExecutor extends AbstractNodeExecutorSupport implements NodeExecutor, SupportsNodeType {

    /**
     * 构造 START 节点执行器。
     *
     * @param objectMapper JSON 对象映射器
     */
    public StartNodeExecutor(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    /**
     * 返回支持的节点类型。
     *
     * @return 节点类型
     */
    @Override
    public WorkflowNodeType supportedNodeType() {
        return WorkflowNodeType.START;
    }

    /**
     * 执行 START 节点。
     *
     * @param context 节点执行上下文
     * @return 节点执行结果
     */
    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        long startedAt = System.nanoTime();
        JsonNode input = context.workflowContext().input();
        validateSchema(context, input, context.nodeDefinition().getInputSchemaRef(), ValidationStage.START_INPUT);
        return NodeExecutionResult.success(input, elapsedMillis(startedAt));
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
