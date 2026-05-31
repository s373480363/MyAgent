package com.myagent.externalagent.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myagent.common.domain.EnableStatus;
import com.myagent.common.error.BizException;
import com.myagent.common.page.PageResult;
import com.myagent.externalagent.application.command.TestExternalAgentCommand;
import com.myagent.externalagent.application.command.UpdateExternalAgentCommand;
import com.myagent.externalagent.application.command.UpdateExternalAgentSecretsCommand;
import com.myagent.externalagent.domain.ExternalAgentType;
import com.myagent.externalagent.repository.ExternalAgentRecord;
import com.myagent.externalagent.repository.ExternalAgentRepository;
import com.myagent.schema.application.query.ListSchemasQuery;
import com.myagent.schema.domain.SchemaCreatedFrom;
import com.myagent.schema.domain.SchemaStatus;
import com.myagent.schema.repository.SchemaRecord;
import com.myagent.schema.repository.SchemaRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 外部 Agent 应用服务测试。
 */
class DefaultExternalAgentApplicationServiceTests {

    /**
     * JSON 对象映射器。
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 详情接口不应回显敏感 header 明文。
     *
     * @throws Exception JSON 构造失败时抛出
     */
    @Test
    void getExternalAgentHidesSensitiveHeaderValues() throws Exception {
        InMemoryExternalAgentRepository repository = new InMemoryExternalAgentRepository();
        ExternalAgentRecord record = repository.insert(new ExternalAgentRecord(
                0L,
                "custom-http",
                ExternalAgentType.CUSTOM_HTTP,
                "HTTP Agent",
                "",
                httpCommandJson("""
                        {
                          "method": "POST",
                          "url": "http://localhost:8081/run",
                          "headers": {
                            "Content-Type": "application/json"
                          },
                          "bodyTemplate": {
                            "prompt": "{prompt}"
                          },
                          "resultSource": {
                            "type": "HTTP_BODY_JSON"
                          },
                          "secretHeaderNames": ["Authorization"],
                          "secretHeaderValues": {
                            "Authorization": "Bearer real-token"
                          }
                        }
                        """),
                "",
                600,
                false,
                false,
                false,
                null,
                EnableStatus.ENABLED,
                null,
                null
        ));
        DefaultExternalAgentApplicationService service = newService(repository);

        var detail = service.getExternalAgent(new com.myagent.externalagent.application.query.GetExternalAgentQuery(record.id()));

        assertThat(detail.commandJson().toString()).doesNotContain("real-token");
        assertThat(detail.commandJson().toString()).doesNotContain("secretHeaderValues");
        assertThat(detail.secretHeaders()).containsExactly(
                new com.myagent.externalagent.application.result.ExternalAgentSecretHeaderResult("Authorization", true)
        );
    }

    /**
     * 普通更新接口不应覆盖已有 secret。
     *
     * @throws Exception JSON 构造失败时抛出
     */
    @Test
    void updateExternalAgentKeepsExistingSecretWhenNormalUpdateDoesNotProvideSecret() throws Exception {
        InMemoryExternalAgentRepository repository = new InMemoryExternalAgentRepository();
        ExternalAgentRecord existing = repository.insert(new ExternalAgentRecord(
                0L,
                "custom-http",
                ExternalAgentType.CUSTOM_HTTP,
                "HTTP Agent",
                "",
                httpCommandJson("""
                        {
                          "method": "POST",
                          "url": "http://localhost:8081/run",
                          "headers": {
                            "Content-Type": "application/json"
                          },
                          "bodyTemplate": {
                            "prompt": "{prompt}"
                          },
                          "resultSource": {
                            "type": "HTTP_BODY_JSON"
                          },
                          "secretHeaderNames": ["Authorization"],
                          "secretHeaderValues": {
                            "Authorization": "Bearer old-token"
                          }
                        }
                        """),
                "",
                600,
                false,
                false,
                false,
                null,
                EnableStatus.ENABLED,
                null,
                null
        ));
        DefaultExternalAgentApplicationService service = newService(repository);

        service.updateExternalAgent(new UpdateExternalAgentCommand(
                existing.id(),
                "HTTP Agent v2",
                "更新描述",
                httpCommandJson("""
                        {
                          "method": "POST",
                          "url": "http://localhost:8081/run2",
                          "headers": {
                            "Content-Type": "application/json"
                          },
                          "bodyTemplate": {
                            "prompt": "{prompt}"
                          },
                          "resultSource": {
                            "type": "HTTP_BODY_JSON"
                          }
                        }
                        """),
                List.of(new UpdateExternalAgentCommand.SecretHeaderItem("Authorization", null)),
                "D:/work",
                300,
                true,
                false,
                false,
                null
        ));

        ExternalAgentRecord updated = repository.findById(existing.id()).orElseThrow();
        assertThat(updated.commandJson().path("secretHeaderValues").path("Authorization").asText())
                .isEqualTo("Bearer old-token");
        assertThat(updated.commandJson().path("secretHeaderNames").get(0).asText()).isEqualTo("Authorization");
    }

    /**
     * /secrets 接口应支持覆盖、清空和保留未出现的旧值。
     *
     * @throws Exception JSON 构造失败时抛出
     */
    @Test
    void updateExternalAgentSecretsSupportsOverwriteClearAndRetain() throws Exception {
        InMemoryExternalAgentRepository repository = new InMemoryExternalAgentRepository();
        ExternalAgentRecord existing = repository.insert(new ExternalAgentRecord(
                0L,
                "custom-http",
                ExternalAgentType.CUSTOM_HTTP,
                "HTTP Agent",
                "",
                httpCommandJson("""
                        {
                          "method": "POST",
                          "url": "http://localhost:8081/run",
                          "headers": {
                            "Content-Type": "application/json"
                          },
                          "bodyTemplate": {},
                          "resultSource": {
                            "type": "HTTP_BODY_JSON"
                          },
                          "secretHeaderNames": ["Authorization", "X-Keep", "X-Legacy"],
                          "secretHeaderValues": {
                            "Authorization": "Bearer old-token",
                            "X-Keep": "keep-token",
                            "X-Legacy": "legacy-token"
                          }
                        }
                        """),
                "",
                600,
                false,
                false,
                false,
                null,
                EnableStatus.ENABLED,
                null,
                null
        ));
        DefaultExternalAgentApplicationService service = newService(repository);

        service.updateExternalAgentSecrets(new UpdateExternalAgentSecretsCommand(
                existing.id(),
                List.of(
                        new UpdateExternalAgentSecretsCommand.Item("Authorization", "Bearer new-token"),
                        new UpdateExternalAgentSecretsCommand.Item("X-New", "new-token")
                ),
                List.of("X-Legacy")
        ));

        ExternalAgentRecord updated = repository.findById(existing.id()).orElseThrow();
        assertThat(updated.commandJson().path("secretHeaderNames").toString())
                .contains("Authorization")
                .contains("X-Keep")
                .contains("X-New")
                .doesNotContain("X-Legacy");
        assertThat(updated.commandJson().path("secretHeaderValues").path("Authorization").asText()).isEqualTo("Bearer new-token");
        assertThat(updated.commandJson().path("secretHeaderValues").path("X-Keep").asText()).isEqualTo("keep-token");
        assertThat(updated.commandJson().path("secretHeaderValues").path("X-New").asText()).isEqualTo("new-token");
        assertThat(updated.commandJson().path("secretHeaderValues").has("X-Legacy")).isFalse();
    }

    /**
     * 测试接口必须在真正发起请求前拒绝缺少 secret 的敏感 header 配置。
     *
     * @throws Exception JSON 构造失败时抛出
     */
    @Test
    void testExternalAgentRejectsMissingSecretBeforeInvocation() throws Exception {
        InMemoryExternalAgentRepository repository = new InMemoryExternalAgentRepository();
        ExternalAgentRecord existing = repository.insert(new ExternalAgentRecord(
                0L,
                "custom-http",
                ExternalAgentType.CUSTOM_HTTP,
                "HTTP Agent",
                "",
                httpCommandJson("""
                        {
                          "method": "POST",
                          "url": "http://localhost:8081/run",
                          "headers": {
                            "Content-Type": "application/json"
                          },
                          "bodyTemplate": {},
                          "resultSource": {
                            "type": "HTTP_BODY_JSON"
                          },
                          "secretHeaderNames": ["Authorization"],
                          "secretHeaderValues": {}
                        }
                        """),
                "",
                600,
                false,
                false,
                false,
                null,
                EnableStatus.ENABLED,
                null,
                null
        ));
        DefaultExternalAgentApplicationService service = newService(repository);

        assertThatThrownBy(() -> service.testExternalAgent(new TestExternalAgentCommand(
                existing.id(),
                "hello",
                OBJECT_MAPPER.createObjectNode()
        ))).isInstanceOf(BizException.class)
                .hasMessageContaining("尚未配置密钥");
    }

    /**
     * 普通更新接口不允许直接写入 secret。
     *
     * @throws Exception JSON 构造失败时抛出
     */
    @Test
    void updateExternalAgentRejectsSecretValueInNormalUpdate() throws Exception {
        InMemoryExternalAgentRepository repository = new InMemoryExternalAgentRepository();
        ExternalAgentRecord existing = repository.insert(new ExternalAgentRecord(
                0L,
                "custom-http",
                ExternalAgentType.CUSTOM_HTTP,
                "HTTP Agent",
                "",
                httpCommandJson("""
                        {
                          "method": "POST",
                          "url": "http://localhost:8081/run",
                          "headers": {
                            "Content-Type": "application/json"
                          },
                          "bodyTemplate": {},
                          "resultSource": {
                            "type": "HTTP_BODY_JSON"
                          },
                          "secretHeaderNames": ["Authorization"],
                          "secretHeaderValues": {
                            "Authorization": "Bearer old-token"
                          }
                        }
                        """),
                "",
                600,
                false,
                false,
                false,
                null,
                EnableStatus.ENABLED,
                null,
                null
        ));
        DefaultExternalAgentApplicationService service = newService(repository);

        assertThatThrownBy(() -> service.updateExternalAgent(new UpdateExternalAgentCommand(
                existing.id(),
                "HTTP Agent",
                "",
                httpCommandJson("""
                        {
                          "method": "POST",
                          "url": "http://localhost:8081/run",
                          "headers": {
                            "Content-Type": "application/json"
                          },
                          "bodyTemplate": {},
                          "resultSource": {
                            "type": "HTTP_BODY_JSON"
                          }
                        }
                        """),
                List.of(new UpdateExternalAgentCommand.SecretHeaderItem("Authorization", "new-token")),
                "",
                600,
                false,
                false,
                false,
                null
        ))).isInstanceOf(BizException.class)
                .hasMessageContaining("/secrets");
    }

    /**
     * 构造应用服务。
     *
     * @param repository 内存仓储
     * @return 应用服务
     */
    private DefaultExternalAgentApplicationService newService(InMemoryExternalAgentRepository repository) {
        ExternalAgentCommandJsonCodec codec = new ExternalAgentCommandJsonCodec(OBJECT_MAPPER);
        return new DefaultExternalAgentApplicationService(
                repository,
                new InMemorySchemaRepository(),
                codec,
                new ExternalAgentTestExecutor(OBJECT_MAPPER, codec)
        );
    }

    /**
     * 构造 HTTP commandJson。
     *
     * @param json JSON 文本
     * @return JSON 节点
     * @throws Exception 解析失败时抛出
     */
    private JsonNode httpCommandJson(String json) throws Exception {
        return OBJECT_MAPPER.readTree(json);
    }

    /**
     * 内存外部 Agent 仓储。
     */
    private static final class InMemoryExternalAgentRepository implements ExternalAgentRepository {

        /**
         * 数据记录。
         */
        private final Map<Long, ExternalAgentRecord> records = new LinkedHashMap<>();

        /**
         * 下一个主键。
         */
        private long nextId = 1L;

        @Override
        public PageResult<ExternalAgentRecord> listExternalAgents(com.myagent.externalagent.application.query.ListExternalAgentsQuery query) {
            return PageResult.of(records.values().stream().toList(), query.page(), query.pageSize(), records.size());
        }

        @Override
        public Optional<ExternalAgentRecord> findById(long adapterId) {
            return Optional.ofNullable(records.get(adapterId));
        }

        @Override
        public Optional<ExternalAgentRecord> findByAdapterKey(String adapterKey) {
            return records.values().stream()
                    .filter(record -> record.adapterKey().equals(adapterKey))
                    .findFirst();
        }

        @Override
        public ExternalAgentRecord insert(ExternalAgentRecord record) {
            long id = nextId++;
            Instant now = Instant.now();
            ExternalAgentRecord inserted = new ExternalAgentRecord(
                    id,
                    record.adapterKey(),
                    record.adapterType(),
                    record.name(),
                    record.description(),
                    record.commandJson().deepCopy(),
                    record.workingDirectory(),
                    record.timeoutSeconds(),
                    record.captureStdout(),
                    record.captureStderr(),
                    record.captureGitDiff(),
                    record.outputSchemaId(),
                    record.status(),
                    now,
                    now
            );
            records.put(id, inserted);
            return inserted;
        }

        @Override
        public void update(ExternalAgentRecord record) {
            ExternalAgentRecord existing = records.get(record.id());
            records.put(record.id(), new ExternalAgentRecord(
                    existing.id(),
                    existing.adapterKey(),
                    existing.adapterType(),
                    record.name(),
                    record.description(),
                    record.commandJson().deepCopy(),
                    record.workingDirectory(),
                    record.timeoutSeconds(),
                    record.captureStdout(),
                    record.captureStderr(),
                    record.captureGitDiff(),
                    record.outputSchemaId(),
                    record.status(),
                    existing.createdAt(),
                    Instant.now()
            ));
        }

        @Override
        public void updateStatus(long adapterId, EnableStatus status) {
            ExternalAgentRecord existing = records.get(adapterId);
            records.put(adapterId, new ExternalAgentRecord(
                    existing.id(),
                    existing.adapterKey(),
                    existing.adapterType(),
                    existing.name(),
                    existing.description(),
                    existing.commandJson().deepCopy(),
                    existing.workingDirectory(),
                    existing.timeoutSeconds(),
                    existing.captureStdout(),
                    existing.captureStderr(),
                    existing.captureGitDiff(),
                    existing.outputSchemaId(),
                    status,
                    existing.createdAt(),
                    Instant.now()
            ));
        }
    }

    /**
     * 内存 Schema 仓储。
     */
    private static final class InMemorySchemaRepository implements SchemaRepository {

        @Override
        public PageResult<SchemaRecord> listSchemas(ListSchemasQuery query) {
            return PageResult.of(List.of(), query.page(), query.pageSize(), 0);
        }

        @Override
        public Optional<SchemaRecord> findById(long schemaId) {
            if (schemaId == 1L) {
                return Optional.of(new SchemaRecord(
                        1L,
                        "system.output",
                        1,
                        "系统输出",
                        "",
                        OBJECT_MAPPER.createObjectNode(),
                        "",
                        SchemaCreatedFrom.SYSTEM_BUILTIN,
                        SchemaStatus.ACTIVE,
                        true,
                        Instant.now(),
                        Instant.now()
                ));
            }
            return Optional.empty();
        }

        @Override
        public Optional<SchemaRecord> findByKeyAndVersion(String schemaKey, int version) {
            return Optional.empty();
        }

        @Override
        public int findMaxVersion(String schemaKey) {
            return 0;
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
