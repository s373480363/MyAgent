package com.myagent.schema.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myagent.common.error.BizException;
import com.myagent.common.page.PageResult;
import com.myagent.schema.application.command.CreateSchemaCommand;
import com.myagent.schema.application.command.CreateSchemaVersionCommand;
import com.myagent.schema.application.command.UpdateSchemaDraftCommand;
import com.myagent.schema.application.query.ListSchemasQuery;
import com.myagent.schema.domain.SchemaCreatedFrom;
import com.myagent.schema.domain.SchemaStatus;
import com.myagent.schema.repository.SchemaRecord;
import com.myagent.schema.repository.SchemaRepository;
import com.myagent.schema.validation.SchemaDefinitionValidator;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Schema 应用服务测试。
 */
class DefaultSchemaApplicationServiceTests {

    /**
     * JSON 对象映射器。
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 创建 Schema 时版本从 1 开始。
     *
     * @throws Exception JSON 构造失败时抛出
     */
    @Test
    void createSchemaStartsVersionFromOne() throws Exception {
        InMemorySchemaRepository repository = new InMemorySchemaRepository();
        DefaultSchemaApplicationService service = newService(repository);

        var result = service.createSchema(new CreateSchemaCommand(
                "agent.input.summary",
                "摘要输入",
                "摘要输入结构",
                objectSchema(),
                "syc.agentstudio.example.SummaryInput",
                SchemaCreatedFrom.AGENT_INPUT,
                null
        ));

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getVersion()).isEqualTo(1);
        assertThat(result.getStatus()).isEqualTo(SchemaStatus.DRAFT);
        assertThat(result.isLocked()).isFalse();
    }

    /**
     * 更新 Schema 草稿时不允许修改非 DRAFT 或已锁定版本。
     *
     * @throws Exception JSON 构造失败时抛出
     */
    @Test
    void updateSchemaDraftRejectsLockedVersion() throws Exception {
        InMemorySchemaRepository repository = new InMemorySchemaRepository();
        SchemaRecord locked = repository.insert(new SchemaRecord(
                0L,
                "agent.input.summary",
                1,
                "摘要输入",
                "",
                objectSchema(),
                "",
                SchemaCreatedFrom.AGENT_INPUT,
                SchemaStatus.ACTIVE,
                true,
                null,
                null
        ));
        DefaultSchemaApplicationService service = newService(repository);

        assertThatThrownBy(() -> service.updateSchemaDraft(new UpdateSchemaDraftCommand(
                locked.getId(),
                "摘要输入 v2",
                "",
                objectSchema(),
                ""
        ))).isInstanceOf(BizException.class)
                .hasMessageContaining("仅 DRAFT 且未锁定");
    }

    /**
     * 创建新版本时复制 schemaKey 并递增版本号。
     *
     * @throws Exception JSON 构造失败时抛出
     */
    @Test
    void createSchemaVersionUsesSameKeyAndNextVersion() throws Exception {
        InMemorySchemaRepository repository = new InMemorySchemaRepository();
        SchemaRecord source = repository.insert(new SchemaRecord(
                0L,
                "agent.input.summary",
                1,
                "摘要输入",
                "",
                objectSchema(),
                "",
                SchemaCreatedFrom.AGENT_INPUT,
                SchemaStatus.ACTIVE,
                true,
                null,
                null
        ));
        DefaultSchemaApplicationService service = newService(repository);

        var result = service.createSchemaVersion(new CreateSchemaVersionCommand(
                source.getId(),
                "摘要输入 v2",
                "新增 language 字段",
                objectSchema(),
                ""
        ));

        assertThat(result.getSchemaKey()).isEqualTo("agent.input.summary");
        assertThat(result.getVersion()).isEqualTo(2);
        assertThat(result.getStatus()).isEqualTo(SchemaStatus.DRAFT);
        assertThat(repository.findById(source.getId()).orElseThrow().getName()).isEqualTo("摘要输入");
    }

    /**
     * 创建服务。
     *
     * @param repository Schema 仓储
     * @return 应用服务
     */
    private DefaultSchemaApplicationService newService(SchemaRepository repository) {
        return new DefaultSchemaApplicationService(repository, new SchemaDefinitionValidator());
    }

    /**
     * 构造对象 Schema。
     *
     * @return JSON Schema
     * @throws Exception JSON 构造失败时抛出
     */
    private JsonNode objectSchema() throws Exception {
        return OBJECT_MAPPER.readTree("""
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
                """);
    }

    /**
     * 内存 Schema 仓储。
     */
    private static final class InMemorySchemaRepository implements SchemaRepository {

        /**
         * 数据记录。
         */
        private final Map<Long, SchemaRecord> records = new LinkedHashMap<>();

        /**
         * 下一个主键。
         */
        private long nextId = 1L;

        @Override
        public PageResult<SchemaRecord> listSchemas(ListSchemasQuery query) {
            return PageResult.of(records.values().stream().toList(), query.page(), query.pageSize(), records.size());
        }

        @Override
        public Optional<SchemaRecord> findById(long schemaId) {
            return Optional.ofNullable(records.get(schemaId));
        }

        @Override
        public Optional<SchemaRecord> findByKeyAndVersion(String schemaKey, int version) {
            return records.values()
                    .stream()
                    .filter(record -> record.getSchemaKey().equals(schemaKey) && record.getVersion() == version)
                    .findFirst();
        }

        @Override
        public int findMaxVersion(String schemaKey) {
            return records.values()
                    .stream()
                    .filter(record -> record.getSchemaKey().equals(schemaKey))
                    .mapToInt(SchemaRecord::getVersion)
                    .max()
                    .orElse(0);
        }

        @Override
        public SchemaRecord insert(SchemaRecord record) {
            long id = nextId++;
            Instant now = Instant.now();
            SchemaRecord inserted = new SchemaRecord(
                    id,
                    record.getSchemaKey(),
                    record.getVersion(),
                    record.getName(),
                    record.getDescription(),
                    record.getJsonSchema(),
                    record.getJavaType(),
                    record.getCreatedFrom(),
                    record.getStatus(),
                    record.isLocked(),
                    now,
                    now
            );
            records.put(id, inserted);
            return inserted;
        }

        @Override
        public int updateDraft(SchemaRecord record) {
            SchemaRecord existing = records.get(record.getId());
            if (existing == null || existing.getStatus() != SchemaStatus.DRAFT || existing.isLocked()) {
                return 0;
            }
            records.put(record.getId(), new SchemaRecord(
                    existing.getId(),
                    existing.getSchemaKey(),
                    existing.getVersion(),
                    record.getName(),
                    record.getDescription(),
                    record.getJsonSchema(),
                    record.getJavaType(),
                    existing.getCreatedFrom(),
                    existing.getStatus(),
                    existing.isLocked(),
                    existing.getCreatedAt(),
                    Instant.now()
            ));
            return 1;
        }

        @Override
        public int lockSchemaVersion(long schemaId) {
            SchemaRecord existing = records.get(schemaId);
            if (existing == null) {
                return 0;
            }
            records.put(schemaId, new SchemaRecord(
                    existing.getId(),
                    existing.getSchemaKey(),
                    existing.getVersion(),
                    existing.getName(),
                    existing.getDescription(),
                    existing.getJsonSchema(),
                    existing.getJavaType(),
                    existing.getCreatedFrom(),
                    SchemaStatus.ACTIVE,
                    true,
                    existing.getCreatedAt(),
                    Instant.now()
            ));
            return 1;
        }
    }
}
