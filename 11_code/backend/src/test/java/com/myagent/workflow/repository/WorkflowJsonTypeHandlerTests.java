package com.myagent.workflow.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.myagent.workflow.domain.ReferencedSchemaVersion;
import com.myagent.workflow.domain.WorkflowRuntimeOptions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 工作流 JSONB 类型序列化测试。
 */
class WorkflowJsonTypeHandlerTests {

    /**
     * JSON 对象映射器。
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * WorkflowRuntimeOptions 必须能从 JSONB 文本稳定读回。
     *
     * @throws Exception JSON 解析失败时抛出
     */
    @Test
    void runtimeOptionsCanBeDeserializedFromJsonText() throws Exception {
        WorkflowRuntimeOptions options = objectMapper.readValue(
                "{\"timeoutSeconds\":600,\"maxSteps\":30,\"maxAgentCallDepth\":3}",
                WorkflowRuntimeOptions.class
        );

        assertThat(options.getTimeoutSeconds()).isEqualTo(600);
        assertThat(options.getMaxSteps()).isEqualTo(30);
        assertThat(options.getMaxAgentCallDepth()).isEqualTo(3);
    }

    /**
     * ReferencedSchemaVersion 列表必须能从 JSONB 文本稳定读回。
     *
     * @throws Exception JSON 解析失败时抛出
     */
    @Test
    void referencedSchemaVersionsCanBeDeserializedFromJsonText() throws Exception {
        List<ReferencedSchemaVersion> references = objectMapper.readValue(
                """
                        [
                          {"schemaId":12,"schemaKey":"summary-output","version":3},
                          {"schemaId":18,"schemaKey":"review-result","version":1}
                        ]
                        """,
                TypeFactory.defaultInstance().constructCollectionType(List.class, ReferencedSchemaVersion.class)
        );

        assertThat(references)
                .extracting(ReferencedSchemaVersion::getSchemaKey)
                .containsExactly("summary-output", "review-result");
        assertThat(references.getFirst().getSchemaId()).isEqualTo(12L);
        assertThat(references.getFirst().getVersion()).isEqualTo(3);
    }
}
