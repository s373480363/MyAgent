package com.myagent.workflow.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.myagent.agent.repository.AgentRecord;
import com.myagent.agent.repository.AgentRepository;
import com.myagent.common.api.ApiError;
import com.myagent.common.domain.EnableStatus;
import com.myagent.common.error.ErrorCode;
import com.myagent.externalagent.repository.ExternalAgentRepository;
import com.myagent.method.repository.JavaMethodRepository;
import com.myagent.schema.repository.SchemaRepository;
import com.myagent.tool.repository.ToolRepository;
import com.myagent.workflow.application.result.WorkflowValidationIssueResult;
import com.myagent.workflow.application.result.WorkflowValidationResult;
import com.myagent.workflow.domain.WorkflowEdgeDefinition;
import com.myagent.workflow.domain.WorkflowNodeDefinition;
import com.myagent.workflow.domain.WorkflowNodeType;
import com.myagent.workflow.domain.WorkflowRuntimeOptions;
import com.myagent.workflow.domain.WorkflowSchemaRef;
import com.myagent.workflow.repository.WorkflowVersionRecord;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 工作流草稿校验服务默认实现。
 */
@Service
public class DefaultWorkflowDraftValidationService implements WorkflowDraftValidationService {

    /**
     * Schema 仓储。
     */
    private final SchemaRepository schemaRepository;

    /**
     * Java 方法仓储。
     */
    private final JavaMethodRepository javaMethodRepository;

    /**
     * 工具仓储。
     */
    private final ToolRepository toolRepository;

    /**
     * 外部 Agent 仓储。
     */
    private final ExternalAgentRepository externalAgentRepository;

    /**
     * Agent 仓储。
     */
    private final AgentRepository agentRepository;

    /**
     * 映射校验服务。
     */
    private final WorkflowMappingValidationService workflowMappingValidationService;

    /**
     * 构造工作流校验服务。
     *
     * @param schemaRepository Schema 仓储
     * @param javaMethodRepository Java 方法仓储
     * @param toolRepository 工具仓储
     * @param externalAgentRepository 外部 Agent 仓储
     * @param agentRepository Agent 仓储
     * @param workflowMappingValidationService 映射校验服务
     */
    public DefaultWorkflowDraftValidationService(
            SchemaRepository schemaRepository,
            JavaMethodRepository javaMethodRepository,
            ToolRepository toolRepository,
            ExternalAgentRepository externalAgentRepository,
            AgentRepository agentRepository,
            WorkflowMappingValidationService workflowMappingValidationService
    ) {
        this.schemaRepository = schemaRepository;
        this.javaMethodRepository = javaMethodRepository;
        this.toolRepository = toolRepository;
        this.externalAgentRepository = externalAgentRepository;
        this.agentRepository = agentRepository;
        this.workflowMappingValidationService = workflowMappingValidationService;
    }

    /**
     * 校验工作流草稿。
     *
     * @param agent Agent 主数据
     * @param workflowVersion 草稿版本
     * @return 校验结果
     */
    @Override
    public WorkflowValidationResult validate(AgentRecord agent, WorkflowVersionRecord workflowVersion) {
        List<WorkflowValidationIssueResult> issues = new ArrayList<>();
        List<WorkflowNodeDefinition> nodes = workflowVersion.nodes() == null ? List.of() : workflowVersion.nodes();
        List<WorkflowEdgeDefinition> edges = workflowVersion.edges() == null ? List.of() : workflowVersion.edges();
        WorkflowRuntimeOptions runtimeOptions = workflowVersion.runtimeOptions();

        validateRuntimeOptions(runtimeOptions, issues);

        Map<String, WorkflowNodeDefinition> nodesById = new HashMap<>();
        Set<String> duplicateNodeIds = new HashSet<>();
        for (WorkflowNodeDefinition node : nodes) {
            if (node == null || isBlank(node.getNodeId())) {
                issues.add(issue("$.nodes", "节点缺少 nodeId。"));
                continue;
            }
            if (nodesById.putIfAbsent(node.getNodeId(), node) != null) {
                duplicateNodeIds.add(node.getNodeId());
            }
        }
        for (String duplicateNodeId : duplicateNodeIds) {
            issues.add(issue("$.nodes", "节点标识必须唯一。", detail("$.nodes[*].nodeId", "duplicate", "重复的节点标识。", duplicateNodeId)));
        }

        Set<String> edgeIds = new HashSet<>();
        for (WorkflowEdgeDefinition edge : edges) {
            if (edge == null || isBlank(edge.getEdgeId())) {
                issues.add(issue("$.edges", "边缺少 edgeId。"));
                continue;
            }
            if (!edgeIds.add(edge.getEdgeId())) {
                issues.add(issue("$.edges", "边标识必须唯一。", detail("$.edges[*].edgeId", "duplicate", "重复的边标识。", edge.getEdgeId())));
            }
            if (isBlank(edge.getSourceNodeId()) || !nodesById.containsKey(edge.getSourceNodeId())) {
                issues.add(issue("$.edges", "边的源节点不存在。", detail("$.edges[*].sourceNodeId", "not_found", "源节点不存在。", edge.getSourceNodeId())));
            }
            if (isBlank(edge.getTargetNodeId()) || !nodesById.containsKey(edge.getTargetNodeId())) {
                issues.add(issue("$.edges", "边的目标节点不存在。", detail("$.edges[*].targetNodeId", "not_found", "目标节点不存在。", edge.getTargetNodeId())));
            }
        }

        long startCount = nodes.stream().filter(node -> node != null && node.getType() == WorkflowNodeType.START).count();
        long endCount = nodes.stream().filter(node -> node != null && node.getType() == WorkflowNodeType.END).count();
        if (startCount != 1) {
            issues.add(issue("$.nodes", "工作流必须且只能存在一个 START 节点。"));
        }
        if (endCount < 1) {
            issues.add(issue("$.nodes", "工作流至少需要一个 END 节点。"));
        }

        Map<String, Integer> incomingCounts = new HashMap<>();
        Map<String, Integer> outgoingCounts = new HashMap<>();
        for (WorkflowEdgeDefinition edge : edges) {
            if (edge == null) {
                continue;
            }
            if (!isBlank(edge.getSourceNodeId())) {
                outgoingCounts.merge(edge.getSourceNodeId(), 1, Integer::sum);
            }
            if (!isBlank(edge.getTargetNodeId())) {
                incomingCounts.merge(edge.getTargetNodeId(), 1, Integer::sum);
            }
        }

        for (WorkflowNodeDefinition node : nodes) {
            if (node == null || node.getType() == null) {
                issues.add(issue("$.nodes", "节点类型不能为空。"));
                continue;
            }
            if (isBlank(node.getName())) {
                issues.add(issue("$.nodes", "节点名称不能为空。", detail("$.nodes[*].name", "required", "节点名称不能为空。", node.getNodeId())));
            }
            if (node.getTimeoutSeconds() != null && node.getTimeoutSeconds() <= 0) {
                issues.add(issue("$.nodes", "节点超时必须大于 0。", detail("$.nodes[*].timeoutSeconds", "invalid", "节点超时必须大于 0。", node.getNodeId())));
            }
            if (!isBlank(node.getFailurePolicy()) && !"FAIL_FAST".equals(node.getFailurePolicy())) {
                issues.add(issue("$.nodes", "当前版本只支持 FAIL_FAST 失败策略。", detail("$.nodes[*].failurePolicy", "not_supported", "当前版本只支持 FAIL_FAST。", node.getFailurePolicy())));
            }
            if (node.getType() != WorkflowNodeType.START && incomingCounts.getOrDefault(node.getNodeId(), 0) == 0) {
                issues.add(issue("$.edges", "除 START 外的节点必须至少有一条入边。", detail("$.nodes[*].nodeId", "missing_incoming_edge", "节点缺少入边。", node.getNodeId())));
            }
            if (node.getType() != WorkflowNodeType.END && outgoingCounts.getOrDefault(node.getNodeId(), 0) == 0) {
                issues.add(issue("$.edges", "除 END 外的节点必须至少有一条出边。", detail("$.nodes[*].nodeId", "missing_outgoing_edge", "节点缺少出边。", node.getNodeId())));
            }

            validateSchemaRef(node, true, node.getInputSchemaRef(), issues);
            validateSchemaRef(node, false, node.getOutputSchemaRef(), issues);
            issues.addAll(workflowMappingValidationService.validateMappings(node));
            validateNodeSpecificRules(agent, node, edges, issues);
        }

        return new WorkflowValidationResult(issues.isEmpty(), issues);
    }

    /**
     * 校验运行约束。
     *
     * @param runtimeOptions 运行约束
     * @param issues 校验问题集合
     */
    private void validateRuntimeOptions(WorkflowRuntimeOptions runtimeOptions, List<WorkflowValidationIssueResult> issues) {
        if (runtimeOptions == null) {
            issues.add(issue("$.runtimeOptions", "runtimeOptions 不能为空。"));
            return;
        }
        if (runtimeOptions.getTimeoutSeconds() <= 0) {
            issues.add(issue("$.runtimeOptions.timeoutSeconds", "工作流总超时必须大于 0。"));
        }
        if (runtimeOptions.getMaxSteps() <= 0) {
            issues.add(issue("$.runtimeOptions.maxSteps", "最大步数必须大于 0。"));
        }
        if (runtimeOptions.getMaxAgentCallDepth() <= 0) {
            issues.add(issue("$.runtimeOptions.maxAgentCallDepth", "最大 Agent 调用深度必须大于 0。"));
        }
    }

    /**
     * 校验 Schema 引用。
     *
     * @param node 节点
     * @param inputRef 是否为输入引用
     * @param schemaRef Schema 引用
     * @param issues 校验问题集合
     */
    private void validateSchemaRef(
            WorkflowNodeDefinition node,
            boolean inputRef,
            WorkflowSchemaRef schemaRef,
            List<WorkflowValidationIssueResult> issues
    ) {
        boolean required = isSchemaRefRequired(node.getType(), inputRef);
        String fieldPath = inputRef ? "$.nodes[*].inputSchemaRef" : "$.nodes[*].outputSchemaRef";
        if (schemaRef == null) {
            if (required) {
                issues.add(issue(fieldPath, "节点缺少必填 Schema 引用。", detail(fieldPath, "required", "该节点缺少必填 Schema 引用。", node.getNodeId())));
            }
            return;
        }
        if (isBlank(schemaRef.getSchemaKey()) || schemaRef.getVersion() == null || schemaRef.getVersion() <= 0) {
            issues.add(issue(fieldPath, "Schema 引用格式不正确。", detail(fieldPath, "invalid", "Schema 引用必须同时包含 schemaKey 和正整数 version。", node.getNodeId())));
            return;
        }
        if (schemaRepository.findByKeyAndVersion(schemaRef.getSchemaKey(), schemaRef.getVersion()).isEmpty()) {
            issues.add(issue(fieldPath, "引用的 Schema 版本不存在。", detail(fieldPath, "not_found", "引用的 Schema 版本不存在。", schemaRef.getSchemaKey() + ":" + schemaRef.getVersion())));
        }
    }

    /**
     * 校验节点特定规则。
     *
     * @param agent Agent 主数据
     * @param node 节点
     * @param edges 边列表
     * @param issues 校验问题集合
     */
    private void validateNodeSpecificRules(
            AgentRecord agent,
            WorkflowNodeDefinition node,
            List<WorkflowEdgeDefinition> edges,
            List<WorkflowValidationIssueResult> issues
    ) {
        switch (node.getType()) {
            case CONDITION -> validateConditionNode(node, edges, issues);
            case LLM, REVIEW, SUMMARY -> validatePromptNode(agent, node, issues);
            case JAVA_METHOD -> validateJavaMethodNode(node, issues);
            case TOOL -> validateToolNode(node, issues);
            case EXTERNAL_AGENT -> validateExternalAgentNode(node, issues);
            case AGENT_CALL -> validateAgentCallNode(agent, node, issues);
            default -> {
                // 其他节点在当前阶段没有额外配置约束。
            }
        }
    }

    /**
     * 校验条件节点。
     *
     * @param node 条件节点
     * @param edges 边列表
     * @param issues 校验问题集合
     */
    private void validateConditionNode(
            WorkflowNodeDefinition node,
            List<WorkflowEdgeDefinition> edges,
            List<WorkflowValidationIssueResult> issues
    ) {
        List<WorkflowEdgeDefinition> outgoingEdges = edges.stream()
                .filter(edge -> edge != null && node.getNodeId().equals(edge.getSourceNodeId()))
                .toList();
        long defaultCount = outgoingEdges.stream()
                .filter(edge -> Boolean.TRUE.equals(edge.getIsDefault()) || edge.getType() == com.myagent.workflow.domain.WorkflowEdgeType.DEFAULT)
                .count();
        if (defaultCount != 1) {
            issues.add(issue("$.edges", "CONDITION 节点必须且只能有一条默认边。", detail("$.edges[*].isDefault", "missing_default_edge", "条件节点缺少默认边或默认边不唯一。", node.getNodeId())));
        }
        for (WorkflowEdgeDefinition edge : outgoingEdges) {
            boolean isDefault = Boolean.TRUE.equals(edge.getIsDefault()) || edge.getType() == com.myagent.workflow.domain.WorkflowEdgeType.DEFAULT;
            if (!isDefault && (edge.getCondition() == null || !edge.getCondition().isObject())) {
                issues.add(issue("$.edges", "CONDITION 节点的显式分支必须配置条件对象。", detail("$.edges[*].condition", "required", "显式条件分支缺少条件对象。", edge.getEdgeId())));
                continue;
            }
            if (!isDefault) {
                validateConditionObject(edge, issues);
            }
        }
    }

    /**
     * 校验条件对象完整形态。
     *
     * @param edge 条件边
     * @param issues 校验问题集合
     */
    private void validateConditionObject(WorkflowEdgeDefinition edge, List<WorkflowValidationIssueResult> issues) {
        JsonNode condition = edge.getCondition();
        String left = textValue(condition, "left");
        if (isBlank(left)) {
            issues.add(issue("$.edges", "CONDITION 分支条件缺少 left。", detail("$.edges[*].condition.left", "required", "条件 left 不能为空。", edge.getEdgeId())));
        } else if (!isControlledJsonPath(left)) {
            issues.add(issue("$.edges", "CONDITION 分支条件 left 不是受控 JSONPath。", detail("$.edges[*].condition.left", "invalid_path", "条件 left 必须是受控 JSONPath。", left)));
        }
        String operator = textValue(condition, "operator");
        if (isBlank(operator)) {
            issues.add(issue("$.edges", "CONDITION 分支条件缺少 operator。", detail("$.edges[*].condition.operator", "required", "条件 operator 不能为空。", edge.getEdgeId())));
        } else if (!Set.of(
                "EXISTS",
                "EQUALS",
                "NOT_EQUALS",
                "CONTAINS",
                "NOT_CONTAINS",
                "IN",
                "NOT_IN",
                "GREATER_THAN",
                "GREATER_THAN_OR_EQUALS",
                "LESS_THAN",
                "LESS_THAN_OR_EQUALS"
        ).contains(operator)) {
            issues.add(issue("$.edges", "CONDITION 分支条件 operator 不支持。", detail("$.edges[*].condition.operator", "not_supported", "不支持的 operator。", operator)));
        }
        String valueType = textValue(condition, "valueType");
        if (isBlank(valueType)) {
            issues.add(issue("$.edges", "CONDITION 分支条件缺少 valueType。", detail("$.edges[*].condition.valueType", "required", "条件 valueType 不能为空。", edge.getEdgeId())));
        } else if (!Set.of("STRING", "NUMBER", "BOOLEAN", "JSON").contains(valueType)) {
            issues.add(issue("$.edges", "CONDITION 分支条件 valueType 不支持。", detail("$.edges[*].condition.valueType", "not_supported", "不支持的 valueType。", valueType)));
        }
        JsonNode right = condition.get("right");
        if (!"EXISTS".equals(operator) && right == null) {
            issues.add(issue("$.edges", "CONDITION 分支条件缺少 right。", detail("$.edges[*].condition.right", "required", "条件 right 不能为空。", edge.getEdgeId())));
        }
        if (("IN".equals(operator) || "NOT_IN".equals(operator)) && right != null && !right.isArray()) {
            issues.add(issue("$.edges", "CONDITION 分支条件 right 必须是数组。", detail("$.edges[*].condition.right", "invalid_type", "IN/NOT_IN 的 right 必须是数组。", edge.getEdgeId())));
        }
    }

    /**
     * 判断是否为受控 JSONPath。
     *
     * @param path JSONPath
     * @return 合法时返回 true
     */
    private boolean isControlledJsonPath(String path) {
        if (isBlank(path) || path.charAt(0) != '$') {
            return false;
        }
        int index = 1;
        while (index < path.length()) {
            char current = path.charAt(index);
            if (current == '.') {
                int start = ++index;
                while (index < path.length() && path.charAt(index) != '.' && path.charAt(index) != '[') {
                    index++;
                }
                if (start == index) {
                    return false;
                }
                continue;
            }
            if (current == '[') {
                int end = path.indexOf(']', index);
                if (end < 0) {
                    return false;
                }
                try {
                    Integer.parseInt(path.substring(index + 1, end));
                } catch (NumberFormatException exception) {
                    return false;
                }
                index = end + 1;
                continue;
            }
            return false;
        }
        return true;
    }

    /**
     * 校验提示词节点。
     *
     * @param agent Agent 主数据
     * @param node 节点
     * @param issues 校验问题集合
     */
    private void validatePromptNode(AgentRecord agent, WorkflowNodeDefinition node, List<WorkflowValidationIssueResult> issues) {
        JsonNode config = node.getConfig();
        if (hasField(config, "prompt") || hasField(config, "promptTemplate")) {
            issues.add(issue("$.nodes", "提示词节点不允许使用旧 prompt/promptTemplate 字段。", detail("$.nodes[*].config", "deprecated_field", "请改用 userPromptTemplate/systemPromptTemplate。", node.getNodeId())));
        }
        if (!hasText(config, "userPromptTemplate")) {
            issues.add(issue("$.nodes", "提示词节点必须配置 userPromptTemplate。", detail("$.nodes[*].config.userPromptTemplate", "missing_prompt", "用户提示词模板缺失。", node.getNodeId())));
        }
    }

    /**
     * 校验 Java 方法节点。
     *
     * @param node 节点
     * @param issues 校验问题集合
     */
    private void validateJavaMethodNode(WorkflowNodeDefinition node, List<WorkflowValidationIssueResult> issues) {
        String methodKey = textValue(node.getConfig(), "methodKey");
        if (isBlank(methodKey)) {
            issues.add(issue("$.nodes", "JAVA_METHOD 节点必须配置 methodKey。", detail("$.nodes[*].config.methodKey", "required", "methodKey 不能为空。", node.getNodeId())));
            return;
        }
        javaMethodRepository.findByMethodKey(methodKey)
                .filter(record -> record.status() == EnableStatus.ENABLED)
                .orElseGet(() -> {
                    issues.add(issue("$.nodes", "JAVA_METHOD 节点引用的方法不存在或未启用。", detail("$.nodes[*].config.methodKey", "not_found", "Java 方法不存在或未启用。", methodKey)));
                    return null;
                });
    }

    /**
     * 校验工具节点。
     *
     * @param node 节点
     * @param issues 校验问题集合
     */
    private void validateToolNode(WorkflowNodeDefinition node, List<WorkflowValidationIssueResult> issues) {
        String toolKey = textValue(node.getConfig(), "toolKey");
        if (isBlank(toolKey)) {
            issues.add(issue("$.nodes", "TOOL 节点必须配置 toolKey。", detail("$.nodes[*].config.toolKey", "required", "toolKey 不能为空。", node.getNodeId())));
            return;
        }
        toolRepository.findByToolKey(toolKey)
                .filter(record -> record.status() == EnableStatus.ENABLED)
                .orElseGet(() -> {
                    issues.add(issue("$.nodes", "TOOL 节点引用的工具不存在或未启用。", detail("$.nodes[*].config.toolKey", "not_found", "工具不存在或未启用。", toolKey)));
                    return null;
                });
    }

    /**
     * 校验外部 Agent 节点。
     *
     * @param node 节点
     * @param issues 校验问题集合
     */
    private void validateExternalAgentNode(WorkflowNodeDefinition node, List<WorkflowValidationIssueResult> issues) {
        String adapterKey = textValue(node.getConfig(), "adapterKey");
        if (isBlank(adapterKey)) {
            issues.add(issue("$.nodes", "EXTERNAL_AGENT 节点必须配置 adapterKey。", detail("$.nodes[*].config.adapterKey", "required", "adapterKey 不能为空。", node.getNodeId())));
            return;
        }
        externalAgentRepository.findByAdapterKey(adapterKey)
                .filter(record -> record.status() == EnableStatus.ENABLED)
                .orElseGet(() -> {
                    issues.add(issue("$.nodes", "EXTERNAL_AGENT 节点引用的适配器不存在或未启用。", detail("$.nodes[*].config.adapterKey", "not_found", "外部 Agent 适配器不存在或未启用。", adapterKey)));
                    return null;
                });
    }

    /**
     * 校验子 Agent 调用节点。
     *
     * @param agent 当前 Agent
     * @param node 节点
     * @param issues 校验问题集合
     */
    private void validateAgentCallNode(AgentRecord agent, WorkflowNodeDefinition node, List<WorkflowValidationIssueResult> issues) {
        String targetAgentKey = textValue(node.getConfig(), "targetAgentKey");
        if (isBlank(targetAgentKey)) {
            issues.add(issue("$.nodes", "AGENT_CALL 节点必须配置 targetAgentKey。", detail("$.nodes[*].config.targetAgentKey", "required", "targetAgentKey 不能为空。", node.getNodeId())));
            return;
        }
        if (targetAgentKey.equals(agent.agentKey())) {
            issues.add(issue("$.nodes", "AGENT_CALL 节点不能直接调用当前 Agent 自己。", detail("$.nodes[*].config.targetAgentKey", "self_call", "不能调用当前 Agent 自己。", targetAgentKey)));
            return;
        }
        agentRepository.findByAgentKey(targetAgentKey)
                .filter(record -> record.status() == EnableStatus.ENABLED && record.currentPublishedWorkflowVersionId() != null)
                .orElseGet(() -> {
                    issues.add(issue("$.nodes", "AGENT_CALL 节点目标 Agent 不存在、未启用或尚未发布。", detail("$.nodes[*].config.targetAgentKey", "not_available", "目标 Agent 不存在、未启用或尚未发布。", targetAgentKey)));
                    return null;
                });
    }

    /**
     * 判断节点的 Schema 引用是否必填。
     *
     * @param nodeType 节点类型
     * @param inputRef 是否输入引用
     * @return 必填时返回 true
     */
    private boolean isSchemaRefRequired(WorkflowNodeType nodeType, boolean inputRef) {
        return switch (nodeType) {
            case START -> inputRef;
            case END -> !inputRef;
            case JAVA_METHOD, TOOL -> true;
            default -> false;
        };
    }

    /**
     * 判断配置中是否存在非空文本字段。
     *
     * @param jsonNode JSON 对象
     * @param fieldName 字段名
     * @return 存在时返回 true
     */
    private boolean hasText(JsonNode jsonNode, String fieldName) {
        String value = textValue(jsonNode, fieldName);
        return !isBlank(value);
    }

    /**
     * 判断配置中是否存在字段。
     *
     * @param jsonNode JSON 对象
     * @param fieldName 字段名
     * @return 字段存在时返回 true
     */
    private boolean hasField(JsonNode jsonNode, String fieldName) {
        return jsonNode != null && jsonNode.isObject() && jsonNode.has(fieldName);
    }

    /**
     * 读取文本字段值。
     *
     * @param jsonNode JSON 对象
     * @param fieldName 字段名
     * @return 文本值
     */
    private String textValue(JsonNode jsonNode, String fieldName) {
        if (jsonNode == null || !jsonNode.isObject()) {
            return null;
        }
        JsonNode value = jsonNode.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText(null);
        return text == null || text.isBlank() ? null : text.trim();
    }

    /**
     * 构造简单校验问题。
     *
     * @param field 字段路径
     * @param message 中文消息
     * @return 校验问题
     */
    private WorkflowValidationIssueResult issue(String field, String message) {
        return new WorkflowValidationIssueResult(
                ErrorCode.WORKFLOW_VALIDATION_FAILED.getCode(),
                message,
                List.of(ApiError.Detail.of(field, "invalid", message))
        );
    }

    /**
     * 构造带明细的校验问题。
     *
     * @param field 字段路径
     * @param message 中文消息
     * @param detail 明细
     * @return 校验问题
     */
    private WorkflowValidationIssueResult issue(String field, String message, ApiError.Detail detail) {
        return new WorkflowValidationIssueResult(
                ErrorCode.WORKFLOW_VALIDATION_FAILED.getCode(),
                message,
                List.of(detail == null ? ApiError.Detail.of(field, "invalid", message) : detail)
        );
    }

    /**
     * 构造字段级错误明细。
     *
     * @param field 字段路径
     * @param reason 错误原因
     * @param message 中文消息
     * @param actual 实际值
     * @return 错误明细
     */
    private ApiError.Detail detail(String field, String reason, String message, String actual) {
        return new ApiError.Detail(field, reason, message, null, actual, null);
    }

    /**
     * 判断字符串是否为空白。
     *
     * @param value 字符串
     * @return 为空白时返回 true
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
