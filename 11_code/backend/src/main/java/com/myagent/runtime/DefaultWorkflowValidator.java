package com.myagent.runtime;

import com.myagent.common.api.ApiError;
import com.myagent.workflow.application.result.WorkflowValidationIssueResult;
import com.myagent.workflow.application.result.WorkflowValidationResult;
import com.myagent.workflow.domain.WorkflowNodeDefinition;
import com.myagent.workflow.domain.WorkflowNodeType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 默认运行时工作流校验器。
 */
@Component
public class DefaultWorkflowValidator implements WorkflowValidator {

    /**
     * 校验草稿版本。
     *
     * @param snapshot 工作流版本快照
     * @return 校验结果
     */
    @Override
    public WorkflowValidationResult validateDraft(WorkflowVersionSnapshot snapshot) {
        return validate(snapshot, false);
    }

    /**
     * 校验发布版本。
     *
     * @param snapshot 工作流版本快照
     * @return 校验结果
     */
    @Override
    public WorkflowValidationResult validateForPublish(WorkflowVersionSnapshot snapshot) {
        return validate(snapshot, true);
    }

    /**
     * 执行通用校验。
     *
     * @param snapshot 工作流版本快照
     * @param requireEnd 是否要求 END 节点
     * @return 校验结果
     */
    private WorkflowValidationResult validate(WorkflowVersionSnapshot snapshot, boolean requireEnd) {
        List<WorkflowValidationIssueResult> errors = new ArrayList<>();
        if (snapshot == null) {
            errors.add(issue("$.workflowVersion", "required", "工作流版本快照不能为空。"));
            return new WorkflowValidationResult(false, errors);
        }
        Set<String> nodeIds = new HashSet<>();
        int startCount = 0;
        int endCount = 0;
        for (WorkflowNodeDefinition node : snapshot.nodes()) {
            if (node == null || node.getNodeId() == null || node.getNodeId().isBlank()) {
                errors.add(issue("$.nodes", "invalid_node", "节点标识不能为空。"));
                continue;
            }
            if (!nodeIds.add(node.getNodeId())) {
                errors.add(issue("$.nodes", "duplicate_node_id", "节点标识重复：" + node.getNodeId()));
            }
            if (node.getType() == WorkflowNodeType.START) {
                startCount++;
            }
            if (node.getType() == WorkflowNodeType.END) {
                endCount++;
            }
        }
        if (startCount != 1) {
            errors.add(issue("$.nodes", "invalid_start_count", "工作流必须且只能有一个 START 节点。"));
        }
        if (requireEnd && endCount < 1) {
            errors.add(issue("$.nodes", "missing_end", "发布工作流至少需要一个 END 节点。"));
        }
        snapshot.edges().forEach(edge -> {
            if (edge.getSourceNodeId() == null || !nodeIds.contains(edge.getSourceNodeId())) {
                errors.add(issue("$.edges", "invalid_source", "边引用了不存在的源节点。"));
            }
            if (edge.getTargetNodeId() == null || !nodeIds.contains(edge.getTargetNodeId())) {
                errors.add(issue("$.edges", "invalid_target", "边引用了不存在的目标节点。"));
            }
        });
        return new WorkflowValidationResult(errors.isEmpty(), errors);
    }

    /**
     * 构造校验问题。
     *
     * @param path 字段路径
     * @param code 问题码
     * @param message 中文消息
     * @return 校验问题
     */
    private WorkflowValidationIssueResult issue(String path, String code, String message) {
        return new WorkflowValidationIssueResult(
                code,
                message,
                List.of(new ApiError.Detail(path, code, message, null, null, null))
        );
    }
}
