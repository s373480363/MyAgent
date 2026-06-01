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

import java.util.Iterator;
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
            if (matches(context, input, edge.getCondition())) {
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
    private boolean matches(NodeExecutionContext context, JsonNode input, JsonNode condition) {
        if (condition == null || !condition.isObject()) {
            throw new BizException(ErrorCode.NODE_EXECUTION_FAILED, "条件分支配置不是对象。");
        }
        String left = requiredText(condition, "left", "条件分支缺少 left。");
        String operator = requiredText(condition, "operator", "条件分支缺少 operator。");
        String valueType = requiredText(condition, "valueType", "条件分支缺少 valueType。");
        JsonNode right = condition.get("right");
        JsonNode actual = context.mappingService().extractInput(input, objectMapper.getNodeFactory().textNode(left));
        return switch (operator) {
            case "EXISTS" -> actual != null && !actual.isMissingNode() && !actual.isNull();
            case "EQUALS" -> Objects.equals(requireTypedValue(actual, valueType, "left"), requireTypedValue(right, valueType, "right"));
            case "NOT_EQUALS" -> !Objects.equals(requireTypedValue(actual, valueType, "left"), requireTypedValue(right, valueType, "right"));
            case "CONTAINS" -> requireTypedValue(actual, "STRING", "actual").asText()
                    .contains(requireTypedValue(right, "STRING", "right").asText());
            case "NOT_CONTAINS" -> !requireTypedValue(actual, "STRING", "left").asText()
                    .contains(requireTypedValue(right, "STRING", "right").asText());
            case "IN" -> inValues(requireTypedValue(actual, valueType, "left"), right, valueType);
            case "NOT_IN" -> !inValues(requireTypedValue(actual, valueType, "left"), right, valueType);
            case "GREATER_THAN" -> requireTypedValue(actual, "NUMBER", "actual").decimalValue()
                    .compareTo(requireTypedValue(right, "NUMBER", "right").decimalValue()) > 0;
            case "GREATER_THAN_OR_EQUALS" -> requireTypedValue(actual, "NUMBER", "actual").decimalValue()
                    .compareTo(requireTypedValue(right, "NUMBER", "right").decimalValue()) >= 0;
            case "LESS_THAN" -> requireTypedValue(actual, "NUMBER", "actual").decimalValue()
                    .compareTo(requireTypedValue(right, "NUMBER", "right").decimalValue()) < 0;
            case "LESS_THAN_OR_EQUALS" -> requireTypedValue(actual, "NUMBER", "actual").decimalValue()
                    .compareTo(requireTypedValue(right, "NUMBER", "right").decimalValue()) <= 0;
            default -> throw new BizException(ErrorCode.NODE_EXECUTION_FAILED, "条件分支不支持的操作符：" + operator);
        };
    }

    /**
     * 判断左值是否在右侧数组中。
     *
     * @param actual 左侧实际值
     * @param expectedArray 右侧字面量数组
     * @param valueType 值类型
     * @return 命中数组时返回 true
     */
    private boolean inValues(JsonNode actual, JsonNode expectedArray, String valueType) {
        if (expectedArray == null || !expectedArray.isArray()) {
            throw new BizException(ErrorCode.NODE_EXECUTION_FAILED, "条件分支 right 必须是数组。");
        }
        Iterator<JsonNode> values = expectedArray.elements();
        while (values.hasNext()) {
            if (Objects.equals(actual, requireTypedValue(values.next(), valueType, "right"))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 读取必填文本字段。
     *
     * @param node 条件对象
     * @param fieldName 字段名
     * @param message 错误消息
     * @return 文本值
     */
    private String requiredText(JsonNode node, String fieldName, String message) {
        JsonNode value = node.get(fieldName);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new BizException(ErrorCode.NODE_EXECUTION_FAILED, message);
        }
        return value.asText();
    }

    /**
     * 要求 JSON 值符合条件声明类型。
     *
     * @param value JSON 值
     * @param valueType 声明类型
     * @param role 值角色
     * @return 原始 JSON 值
     */
    private JsonNode requireTypedValue(JsonNode value, String valueType, String role) {
        if (value == null || value.isMissingNode() || value.isNull()) {
            throw new BizException(ErrorCode.NODE_EXECUTION_FAILED, "条件分支 " + role + " 值缺失。");
        }
        boolean matched = switch (valueType) {
            case "STRING" -> value.isTextual();
            case "NUMBER" -> value.isNumber();
            case "BOOLEAN" -> value.isBoolean();
            case "JSON" -> true;
            default -> throw new BizException(ErrorCode.NODE_EXECUTION_FAILED, "条件分支不支持的 valueType：" + valueType);
        };
        if (!matched) {
            throw new BizException(ErrorCode.NODE_EXECUTION_FAILED, "条件分支 " + role + " 类型不匹配：" + valueType);
        }
        return value;
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
