package com.myagent.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myagent.agent.repository.AgentRecord;
import com.myagent.common.domain.EnableStatus;
import com.myagent.run.domain.RunStatus;
import com.myagent.settings.domain.PlatformSettingsResolver;
import com.myagent.workflow.domain.WorkflowNodeDefinition;
import com.myagent.workflow.domain.WorkflowEdgeDefinition;
import com.myagent.workflow.domain.WorkflowNodeType;
import com.myagent.workflow.domain.WorkflowRuntimeOptions;
import com.myagent.workflow.domain.WorkflowVersionStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 默认工作流运行引擎测试。
 */
class DefaultWorkflowRuntimeEngineTests {

    /**
     * JSON 对象映射器。
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 节点级超时必须由运行引擎真实包住节点执行。
     *
     * @throws Exception JSON 解析失败时抛出
     */
    @Test
    void executeReturnsTimeoutWhenNodeExecutionExceedsNodeTimeout() throws Exception {
        WorkflowNodeDefinition slowNode = new WorkflowNodeDefinition();
        slowNode.setNodeId("slow-llm");
        slowNode.setName("慢节点");
        slowNode.setType(WorkflowNodeType.LLM);
        slowNode.setTimeoutSeconds(1);
        WorkflowVersionSnapshot snapshot = new WorkflowVersionSnapshot(
                10L,
                1L,
                "agent",
                "Agent",
                1,
                WorkflowVersionStatus.PUBLISHED,
                List.of(startNode(), slowNode),
                List.of(edge("start", slowNode.getNodeId())),
                new WorkflowRuntimeOptions(600, 30, 3),
                List.of()
        );
        WorkflowCompiler compiler = new DefaultWorkflowCompiler();
        NodeExecutorRegistry registry = mock(NodeExecutorRegistry.class);
        when(registry.getExecutor(WorkflowNodeType.START)).thenReturn(context ->
                NodeExecutionResult.success(objectMapper.createObjectNode(), 1));
        when(registry.getExecutor(WorkflowNodeType.LLM)).thenReturn(context -> {
            try {
                Thread.sleep(2_000);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
            return NodeExecutionResult.success(objectMapper.createObjectNode().put("late", true), 2_000);
        });
        CapturingTraceWriter traceWriter = new CapturingTraceWriter();
        DefaultMappingService mappingService = new DefaultMappingService(objectMapper);
        DefaultRuntimeLimitGuard limitGuard = new DefaultRuntimeLimitGuard();
        DefaultWorkflowRuntimeEngine engine = new DefaultWorkflowRuntimeEngine(
                objectMapper,
                compiler,
                traceWriter,
                mappingService,
                limitGuard,
                nodeExecutionRunner(registry, traceWriter, mappingService, limitGuard)
        );

        WorkflowRuntimeResult result = engine.execute(
                100L,
                "run-timeout",
                agent(),
                snapshot,
                objectMapper.createObjectNode()
        );

        assertThat(result.status()).isEqualTo(RunStatus.TIMEOUT);
        assertThat(result.errorCode()).isEqualTo("RUN_TIMEOUT");
        assertThat(traceWriter.finishedNodeRun.get().status()).isEqualTo(RunStatus.TIMEOUT);
    }

    /**
     * 运行总超时必须能包住未配置节点级超时的阻塞节点。
     *
     * @throws Exception JSON 解析失败时抛出
     */
    @Test
    void executeReturnsTimeoutWhenNodeExecutionExceedsRunTimeout() throws Exception {
        WorkflowNodeDefinition slowNode = new WorkflowNodeDefinition();
        slowNode.setNodeId("slow-end");
        slowNode.setName("慢结束节点");
        slowNode.setType(WorkflowNodeType.END);
        WorkflowVersionSnapshot snapshot = new WorkflowVersionSnapshot(
                10L,
                1L,
                "agent",
                "Agent",
                1,
                WorkflowVersionStatus.PUBLISHED,
                List.of(startNode(), slowNode),
                List.of(edge("start", slowNode.getNodeId())),
                new WorkflowRuntimeOptions(1, 30, 3),
                List.of()
        );
        WorkflowCompiler compiler = new DefaultWorkflowCompiler();
        NodeExecutorRegistry registry = mock(NodeExecutorRegistry.class);
        when(registry.getExecutor(WorkflowNodeType.START)).thenReturn(context ->
                NodeExecutionResult.success(objectMapper.createObjectNode(), 1));
        when(registry.getExecutor(WorkflowNodeType.END)).thenReturn(context -> {
            try {
                Thread.sleep(2_000);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
            return NodeExecutionResult.success(objectMapper.createObjectNode().put("late", true), 2_000);
        });
        CapturingTraceWriter traceWriter = new CapturingTraceWriter();
        DefaultMappingService mappingService = new DefaultMappingService(objectMapper);
        DefaultRuntimeLimitGuard limitGuard = new DefaultRuntimeLimitGuard();
        DefaultWorkflowRuntimeEngine engine = new DefaultWorkflowRuntimeEngine(
                objectMapper,
                compiler,
                traceWriter,
                mappingService,
                limitGuard,
                nodeExecutionRunner(registry, traceWriter, mappingService, limitGuard)
        );

        WorkflowRuntimeResult result = engine.execute(
                100L,
                "run-timeout",
                agent(),
                snapshot,
                objectMapper.createObjectNode()
        );

        assertThat(result.status()).isEqualTo(RunStatus.TIMEOUT);
        assertThat(result.errorCode()).isEqualTo("RUN_TIMEOUT");
        assertThat(traceWriter.finishedNodeRun.get().status()).isEqualTo(RunStatus.TIMEOUT);
    }

    /**
     * 构造 Agent 记录。
     *
     * @return Agent 记录
     */
    private AgentRecord agent() {
        return new AgentRecord(
                1L,
                "agent",
                "Agent",
                "",
                EnableStatus.ENABLED,
                "",
                "gpt-4.1-mini",
                BigDecimal.ZERO,
                600,
                30,
                null,
                10L,
                Instant.now(),
                Instant.now()
        );
    }

    /**
     * 构造 START 节点。
     *
     * @return START 节点
     */
    private WorkflowNodeDefinition startNode() {
        WorkflowNodeDefinition node = new WorkflowNodeDefinition();
        node.setNodeId("start");
        node.setName("开始");
        node.setType(WorkflowNodeType.START);
        return node;
    }

    /**
     * 构造普通边。
     *
     * @param source 源节点
     * @param target 目标节点
     * @return 边定义
     */
    private WorkflowEdgeDefinition edge(String source, String target) {
        WorkflowEdgeDefinition edge = new WorkflowEdgeDefinition();
        edge.setEdgeId(source + "-" + target);
        edge.setSourceNodeId(source);
        edge.setTargetNodeId(target);
        return edge;
    }

    /**
     * 构造节点执行协调器。
     *
     * @param registry 节点执行器注册表
     * @param traceWriter Trace 写入器
     * @param mappingService 映射服务
     * @param limitGuard 运行限制守卫
     * @return 节点执行协调器
     */
    private NodeExecutionRunner nodeExecutionRunner(
            NodeExecutorRegistry registry,
            TraceWriter traceWriter,
            MappingService mappingService,
            RuntimeLimitGuard limitGuard
    ) {
        return new NodeExecutionRunner(
                objectMapper,
                registry,
                (payload, schemaRef, stage) -> com.myagent.schema.validation.SchemaValidationResult.valid("test", 1),
                mappingService,
                limitGuard,
                mock(PlatformSettingsResolver.class),
                new ActiveChildRunRegistry(mock(com.myagent.run.repository.AgentRunRepository.class)),
                Executors.newCachedThreadPool()
        );
    }

    /**
     * 采集 Trace 写入内容的测试实现。
     */
    private static final class CapturingTraceWriter implements TraceWriter {

        /**
         * 完成的节点运行记录。
         */
        private final AtomicReference<NodeRunFinishRecord> finishedNodeRun = new AtomicReference<>();

        @Override
        public NodeRunStartResult createNodeRun(NodeRunStartRecord record) {
            return new NodeRunStartResult(200L, record.agentRunDbId(), record.agentRunNo(), record.nodeId(), Instant.now());
        }

        @Override
        public void finishNodeRun(NodeRunFinishRecord record) {
            finishedNodeRun.set(record);
        }

        @Override
        public void writeEvent(TraceEventRecord record) {
            // 本测试只断言节点运行状态，Trace 事件由运行详情链路测试覆盖。
        }
    }
}
