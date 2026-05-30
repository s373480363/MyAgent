package com.myagent.schema.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myagent.common.page.PageResult;
import com.myagent.schema.application.query.ListSchemasQuery;
import com.myagent.schema.domain.SchemaCreatedFrom;
import com.myagent.schema.domain.SchemaStatus;
import com.myagent.schema.repository.SchemaRecord;
import com.myagent.schema.repository.SchemaRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Schema 运行时校验服务测试。
 */
class DefaultSchemaValidationServiceTests {

    /**
     * JSON 对象映射器。
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 校验失败时返回字段路径和中文错误。
     *
     * @throws Exception JSON 构造失败时抛出
     */
    @Test
    void validateReturnsFieldPathErrors() throws Exception {
        DefaultSchemaValidationService service = new DefaultSchemaValidationService(new SingleSchemaRepository(schemaRecord()));
        JsonNode payload = OBJECT_MAPPER.readTree("""
                {
                  "extra": true
                }
                """);

        SchemaValidationResult result = service.validate(payload, SchemaReference.byId(1L), ValidationStage.START_INPUT);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getSchemaKey()).isEqualTo("agent.input.summary");
        assertThat(result.getErrors())
                .extracting(SchemaValidationError::getPath)
                .contains("$.question", "$.extra");
    }

    /**
     * 校验通过时没有错误。
     *
     * @throws Exception JSON 构造失败时抛出
     */
    @Test
    void validateReturnsValidWhenPayloadMatchesSchema() throws Exception {
        DefaultSchemaValidationService service = new DefaultSchemaValidationService(new SingleSchemaRepository(schemaRecord()));
        JsonNode payload = OBJECT_MAPPER.readTree("""
                {
                  "question": "请总结文本"
                }
                """);

        SchemaValidationResult result = service.validate(payload, SchemaReference.byId(1L), ValidationStage.START_INPUT);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    /**
     * 构造测试 Schema。
     *
     * @return Schema 记录
     * @throws Exception JSON 构造失败时抛出
     */
    private SchemaRecord schemaRecord() throws Exception {
        return new SchemaRecord(
                1L,
                "agent.input.summary",
                1,
                "摘要输入",
                "",
                OBJECT_MAPPER.readTree("""
                        {
                          "type": "object",
                          "properties": {
                            "question": {
                              "type": "string"
                            }
                          },
                          "required": ["question"],
                          "additionalProperties": false
                        }
                        """),
                "",
                SchemaCreatedFrom.AGENT_INPUT,
                SchemaStatus.DRAFT,
                false,
                Instant.now(),
                Instant.now()
        );
    }

    /**
     * 单记录 Schema 仓储。
     */
    private record SingleSchemaRepository(SchemaRecord record) implements SchemaRepository {

        @Override
        public PageResult<SchemaRecord> listSchemas(ListSchemasQuery query) {
            return PageResult.of(java.util.List.of(record), 1, 20, 1);
        }

        @Override
        public Optional<SchemaRecord> findById(long schemaId) {
            return schemaId == record.getId() ? Optional.of(record) : Optional.empty();
        }

        @Override
        public Optional<SchemaRecord> findByKeyAndVersion(String schemaKey, int version) {
            if (record.getSchemaKey().equals(schemaKey) && record.getVersion() == version) {
                return Optional.of(record);
            }
            return Optional.empty();
        }

        @Override
        public int findMaxVersion(String schemaKey) {
            return record.getVersion();
        }

        @Override
        public SchemaRecord insert(SchemaRecord record) {
            return record;
        }

        @Override
        public int updateDraft(SchemaRecord record) {
            return 0;
        }

        @Override
        public int lockSchemaVersion(long schemaId) {
            return 0;
        }
    }
}
