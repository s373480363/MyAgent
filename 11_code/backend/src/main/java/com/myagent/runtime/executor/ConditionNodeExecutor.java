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
import com.myagent.workflow.domain.WorkflowEdgeDefinition;
import com.myagent.workflow.domain.WorkflowEdgeType;
import com.myagent.workflow.domain.WorkflowNodeType;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * CONDITION 节点执行器。
 */
@Component
public class ConditionNodeExecutor extends AbstractNodeExecutorSupport implements NodeExecutor, SupportsNodeType {

    /**
     * 构造 CONDITION 节点执行器。
     *
     * @param objectMapper JSON 对象映射器
     */
    public ConditionNodeExecutor(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    /**
     * 返回支持的节点类型。
     *
     * @return 节点类型
     */
    @Override
    public WorkflowNodeType supportedNodeType() {
        return WorkflowNodeType.CONDITION;
    }

    /**
     * 执行 CONDITION 节点。
     *
     * @param context 节点执行上下文
     * @return 节点执行结果
     */
    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        long startedAt = System.nanoTime();
        JsonNode input = extractInput(context);
        WorkflowEdgeDefinition selected = selectEdge(context, input);
        context.traceWriter().writeEvent(new TraceEventRecord(
                context.agentRunDbId(),
                context.nodeRunDbId(),
                null,
                TraceEventType.CONDITION_DECISION,
                "条件节点命中分支：" + selected.getEdgeId(),
                objectMapper.valueToTree(selected)
        ));
        return NodeExecutionResult.success(input, selected.getEdgeId(), elapsedMillis(startedAt));
    }

    /**
     * 选择条件边。
     *
     * @param context 节点执行上下文
     * @param input 节点输入
     * @return 命中的边
     */
    private WorkflowEdgeDefinition selectEdge(NodeExecutionContext context, JsonNode input) {
        WorkflowEdgeDefinition defaultEdge = null;
        for (WorkflowEdgeDefinition edge : context.outgoingEdges()) {
            if (Boolean.TRUE.equals(edge.getIsDefault()) || edge.getType() == WorkflowEdgeType.DEFAULT) {
                defaultEdge = edge;
                continue;
            }
            if (matches(input, edge.getCondition())) {
                return edge;
            }
        }
        if (defaultEdge != null) {
            return defaultEdge;
        }
        throw new BizException(ErrorCode.NODE_EXECUTION_FAILED, "条件节点没有命中分支，且未配置默认分支。");
    }

    /**
     * 判断条件是否命中。
     *
     * @param input 节点输入
     * @param condition 条件对象
     * @return 命中时返回 true
     */
    private boolean matches(JsonNode input, JsonNode condition) {
        if (condition == null || !condition.isObject()) {
            return false;
        }
        String path = condition.path("path").asText(null);
        String operator = condition.path("operator").asText("EQUALS");
        JsonNode expected = condition.get("value");
        JsonNode actual = path == null ? input : new com.myagent.runtime.DefaultMappingService(objectMapper).extractInput(input, objectMapper.getNodeFactory().textNode(path));
        return switch (operator) {
            case "EXISTS" -> actual != null && !actual.isMissingNode() && !actual.isNull();
            case "NOT_EQUALS" -> !Objects.equals(actual, expected);
            case "CONTAINS" -> actual != null && actual.asText("").contains(expected == null ? "" : expected.asText());
            default -> Objects.equals(actual, expected);
        };
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
