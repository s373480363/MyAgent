package com.myagent.runtime;

import com.myagent.common.error.BizException;
import com.myagent.workflow.domain.WorkflowEdgeDefinition;
import com.myagent.workflow.domain.WorkflowNodeDefinition;
import com.myagent.workflow.domain.WorkflowNodeType;
import com.myagent.workflow.domain.WorkflowRuntimeOptions;
import com.myagent.workflow.domain.WorkflowVersionStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 默认工作流编译器测试。
 */
class DefaultWorkflowCompilerTests {

    /**
     * 编译器。
     */
    private final DefaultWorkflowCompiler compiler = new DefaultWorkflowCompiler();

    /**
     * 编译器按节点和出边生成稳定索引。
     */
    @Test
    void compileBuildsNodeAndEdgeIndexes() {
        WorkflowNodeDefinition start = node("start", WorkflowNodeType.START);
        WorkflowNodeDefinition end = node("end", WorkflowNodeType.END);
        WorkflowEdgeDefinition edge = new WorkflowEdgeDefinition();
        edge.setEdgeId("edge-1");
        edge.setSourceNodeId("start");
        edge.setTargetNodeId("end");

        CompiledWorkflow compiled = compiler.compile(snapshot(List.of(start, end), List.of(edge)));

        assertThat(compiled.startNode().getNodeId()).isEqualTo("start");
        assertThat(compiled.getNode("end").getType()).isEqualTo(WorkflowNodeType.END);
        assertThat(compiled.getOutgoingEdges("start")).hasSize(1);
    }

    /**
     * 编译器必须拒绝多个 START 节点。
     */
    @Test
    void compileRejectsDuplicateStartNodes() {
        WorkflowNodeDefinition startOne = node("start-1", WorkflowNodeType.START);
        WorkflowNodeDefinition startTwo = node("start-2", WorkflowNodeType.START);

        assertThatThrownBy(() -> compiler.compile(snapshot(List.of(startOne, startTwo), List.of())))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("只能存在一个 START 节点");
    }

    /**
     * 构造工作流快照。
     *
     * @param nodes 节点
     * @param edges 边
     * @return 工作流快照
     */
    private WorkflowVersionSnapshot snapshot(List<WorkflowNodeDefinition> nodes, List<WorkflowEdgeDefinition> edges) {
        return new WorkflowVersionSnapshot(
                1L,
                1L,
                "demo-agent",
                "演示 Agent",
                1,
                WorkflowVersionStatus.DRAFT,
                nodes,
                edges,
                new WorkflowRuntimeOptions(600, 30, 3),
                List.of()
        );
    }

    /**
     * 构造节点定义。
     *
     * @param nodeId 节点标识
     * @param nodeType 节点类型
     * @return 节点定义
     */
    private WorkflowNodeDefinition node(String nodeId, WorkflowNodeType nodeType) {
        WorkflowNodeDefinition node = new WorkflowNodeDefinition();
        node.setNodeId(nodeId);
        node.setName(nodeId);
        node.setType(nodeType);
        return node;
    }
}
