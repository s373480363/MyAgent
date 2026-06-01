package com.myagent.workflow.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myagent.agent.repository.AgentRecord;
import com.myagent.common.domain.EnableStatus;
import com.myagent.externalagent.repository.ExternalAgentRepository;
import com.myagent.method.repository.JavaMethodRepository;
import com.myagent.schema.domain.SchemaCreatedFrom;
import com.myagent.schema.domain.SchemaStatus;
import com.myagent.schema.repository.SchemaRecord;
import com.myagent.schema.repository.SchemaRepository;
import com.myagent.tool.repository.ToolRepository;
import com.myagent.workflow.application.result.WorkflowValidationResult;
import com.myagent.workflow.domain.WorkflowEdgeDefinition;
import com.myagent.workflow.domain.WorkflowEdgeType;
import com.myagent.workflow.domain.WorkflowNodeDefinition;
import com.myagent.workflow.domain.WorkflowNodeType;
import com.myagent.workflow.domain.WorkflowRuntimeOptions;
import com.myagent.workflow.domain.WorkflowSchemaRef;
import com.myagent.workflow.domain.WorkflowVersionStatus;
import com.myagent.workflow.repository.WorkflowVersionRecord;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 工作流草稿发布校验测试。
 */
class DefaultWorkflowDraftValidationServiceTests {

    /**
     * JSON 对象映射器。
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * inputMapping 目标字段不存在于 inputSchema 时必须失败。
     *
     * @throws Exception JSON 构造失败时抛出
     */
    @Test
    void validateRejectsInputMappingFieldOutsideInputSchema() throws Exception {
        WorkflowNodeDefinition llm = llmNode();
        llm.setInputMapping(OBJECT_MAPPER.readTree("""
                {
                  "missing": "$.input.question"
                }
                """));

        WorkflowValidationResult result = validate(llm);

        assertThat(result.isValid()).isFalse();
        assertThat(messages(result)).anyMatch(message -> message.contains("inputMapping 目标字段不存在"));
    }

    /**
     * inputMapping 缺少 inputSchema 必填字段时必须失败。
     *
     * @throws Exception JSON 构造失败时抛出
     */
    @Test
    void validateRejectsInputMappingMissingRequiredField() throws Exception {
        WorkflowNodeDefinition llm = llmNode();
        llm.setInputMapping(OBJECT_MAPPER.readTree("""
                {
                  "content": "$.input.content"
                }
                """));

        WorkflowValidationResult result = validate(llm);

        assertThat(result.isValid()).isFalse();
        assertThat(messages(result)).anyMatch(message -> message.contains("缺少 inputSchema 必填字段：question"));
    }

    /**
     * outputMapping 读取 outputSchema 不存在字段时必须失败。
     *
     * @throws Exception JSON 构造失败时抛出
     */
    @Test
    void validateRejectsOutputMappingSourceOutsideOutputSchema() throws Exception {
        WorkflowNodeDefinition llm = llmNode();
        llm.setInputMapping(OBJECT_MAPPER.readTree("""
                {
                  "question": "$.input.question"
                }
                """));
        llm.setOutputMapping(OBJECT_MAPPER.readTree("""
                {
                  "$.result.summary": "$.missing"
                }
                """));

        WorkflowValidationResult result = validate(llm);

        assertThat(result.isValid()).isFalse();
        assertThat(messages(result)).anyMatch(message -> message.contains("outputMapping 来源字段不存在"));
    }

    /**
     * outputMapping 写入 $.input 时必须失败。
     *
     * @throws Exception JSON 构造失败时抛出
     */
    @Test
    void validateRejectsOutputMappingWritingInputSubtree() throws Exception {
        WorkflowNodeDefinition llm = llmNode();
        llm.setInputMapping(OBJECT_MAPPER.readTree("""
                {
                  "question": "$.input.question"
                }
                """));
        llm.setOutputMapping(OBJECT_MAPPER.readTree("""
                {
                  "$.input.summary": "$.summary"
                }
                """));

        WorkflowValidationResult result = validate(llm);

        assertThat(result.isValid()).isFalse();
        assertThat(messages(result)).anyMatch(message -> message.contains("不允许写入 $.input"));
    }

    /**
     * 合法 inputMapping 和 outputMapping 应通过发布校验。
     *
     * @throws Exception JSON 构造失败时抛出
     */
    @Test
    void validateAcceptsSchemaAlignedMappings() throws Exception {
        WorkflowNodeDefinition llm = llmNode();
        llm.setInputMapping(OBJECT_MAPPER.readTree("""
                {
                  "question": "$.input.question"
                }
                """));
        llm.setOutputMapping(OBJECT_MAPPER.readTree("""
                {
                  "$.result.summary": "$.summary"
                }
                """));

        WorkflowValidationResult result = validate(llm);

        assertThat(result.isValid()).isTrue();
    }

    /**
     * 执行校验。
     *
     * @param llm LLM 节点
     * @return 校验结果
     * @throws Exception JSON 构造失败时抛出
     */
    private WorkflowValidationResult validate(WorkflowNodeDefinition llm) throws Exception {
        SchemaRepository schemaRepository = schemaRepository();
        DefaultWorkflowDraftValidationService service = new DefaultWorkflowDraftValidationService(
                schemaRepository,
                mock(JavaMethodRepository.class),
                mock(ToolRepository.class),
                mock(ExternalAgentRepository.class),
                mock(com.myagent.agent.repository.AgentRepository.class),
                new WorkflowMappingValidationService(schemaRepository)
        );
        return service.validate(agent(), new WorkflowVersionRecord(
                1L,
                1L,
                1,
                WorkflowVersionStatus.DRAFT,
                List.of(startNode(), llm, endNode()),
                List.of(edge("e1", "start", "llm"), edge("e2", "llm", "end")),
                new WorkflowRuntimeOptions(600, 30, 3),
                List.of(),
                null,
                null,
                Instant.now(),
                Instant.now()
        ));
    }

    /**
     * 构造 Schema 仓储。
     *
     * @return Schema 仓储
     * @throws Exception JSON 构造失败时抛出
     */
    private SchemaRepository schemaRepository() throws Exception {
        SchemaRepository repository = mock(SchemaRepository.class);
        when(repository.findByKeyAndVersion("input", 1)).thenReturn(Optional.of(schemaRecord(
                1L,
                "input",
                """
                        {
                          "type": "object",
                          "required": ["question"],
                          "properties": {
                            "question": { "type": "string" },
                            "content": { "type": "string" }
                          }
                        }
                        """
        )));
        when(repository.findByKeyAndVersion("output", 1)).thenReturn(Optional.of(schemaRecord(
                2L,
                "output",
                """
                        {
                          "type": "object",
                          "properties": {
                            "summary": { "type": "string" },
                            "score": { "type": "number" }
                          }
                        }
                        """
        )));
        return repository;
    }

    /**
     * 构造 Schema 记录。
     *
     * @param id 主键
     * @param schemaKey Schema 业务键
     * @param schemaJson Schema JSON 文本
     * @return Schema 记录
     * @throws Exception JSON 构造失败时抛出
     */
    private SchemaRecord schemaRecord(long id, String schemaKey, String schemaJson) throws Exception {
        return new SchemaRecord(
                id,
                schemaKey,
                1,
                schemaKey,
                "",
                OBJECT_MAPPER.readTree(schemaJson),
                "",
                SchemaCreatedFrom.USER_CREATED,
                SchemaStatus.ACTIVE,
                false,
                Instant.now(),
                Instant.now()
        );
    }

    /**
     * 构造 Agent。
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
                "系统提示词",
                "gpt-4.1-mini",
                null,
                600,
                30,
                null,
                null,
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
        node.setType(WorkflowNodeType.START);
        node.setName("开始");
        node.setInputSchemaRef(schemaRef("input"));
        return node;
    }

    /**
     * 构造 LLM 节点。
     *
     * @return LLM 节点
     * @throws Exception JSON 构造失败时抛出
     */
    private WorkflowNodeDefinition llmNode() throws Exception {
        WorkflowNodeDefinition node = new WorkflowNodeDefinition();
        node.setNodeId("llm");
        node.setType(WorkflowNodeType.LLM);
        node.setName("LLM");
        node.setInputSchemaRef(schemaRef("input"));
        node.setOutputSchemaRef(schemaRef("output"));
        node.setConfig(OBJECT_MAPPER.readTree("""
                {
                  "userPromptTemplate": "请处理 {inputJson}"
                }
                """));
        return node;
    }

    /**
     * 构造 END 节点。
     *
     * @return END 节点
     */
    private WorkflowNodeDefinition endNode() {
        WorkflowNodeDefinition node = new WorkflowNodeDefinition();
        node.setNodeId("end");
        node.setType(WorkflowNodeType.END);
        node.setName("结束");
        node.setOutputSchemaRef(schemaRef("output"));
        return node;
    }

    /**
     * 构造 Schema 引用。
     *
     * @param schemaKey Schema 业务键
     * @return Schema 引用
     */
    private WorkflowSchemaRef schemaRef(String schemaKey) {
        WorkflowSchemaRef schemaRef = new WorkflowSchemaRef();
        schemaRef.setSchemaKey(schemaKey);
        schemaRef.setVersion(1);
        return schemaRef;
    }

    /**
     * 构造普通边。
     *
     * @param edgeId 边标识
     * @param source 源节点
     * @param target 目标节点
     * @return 边定义
     */
    private WorkflowEdgeDefinition edge(String edgeId, String source, String target) {
        WorkflowEdgeDefinition edge = new WorkflowEdgeDefinition();
        edge.setEdgeId(edgeId);
        edge.setSourceNodeId(source);
        edge.setTargetNodeId(target);
        edge.setType(WorkflowEdgeType.NORMAL);
        return edge;
    }

    /**
     * 提取校验消息。
     *
     * @param result 校验结果
     * @return 消息列表
     */
    private List<String> messages(WorkflowValidationResult result) {
        return result.getErrors().stream()
                .map(error -> error.getMessage())
                .toList();
    }
}
