package com.myagent.database;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * PostgreSQL 迁移集成测试。
 */
@Testcontainers(disabledWithoutDocker = true)
class PostgresMigrationTests {

    /**
     * V1 核心表清单。
     */
    private static final List<String> CORE_TABLES = List.of(
            "schema_definition",
            "java_method_definition",
            "tool_definition",
            "external_agent_definition",
            "agent_definition",
            "workflow_version",
            "system_setting",
            "agent_run",
            "node_run",
            "trace_event",
            "agent_message",
            "eval_suite",
            "eval_case",
            "eval_run",
            "eval_case_result"
    );

    /**
     * PostgreSQL 测试容器。
     */
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("myagent_test")
                    .withUsername("myagent")
                    .withPassword("myagent");

    /**
     * 验证 Flyway 可以把空 PostgreSQL 数据库迁移到完整 V1 结构。
     *
     * @throws SQLException 数据库访问失败时抛出
     */
    @Test
    void flywayMigratesEmptyPostgresDatabase() throws SQLException {
        migrate();

        try (Connection connection = openConnection()) {
            // 真实读取 PostgreSQL 元数据，确认所有核心表已经存在。
            for (String tableName : CORE_TABLES) {
                assertThat(tableExists(connection, tableName)).isTrue();
            }

            assertThat(indexExists(connection, "uq_workflow_version_one_draft")).isTrue();
            assertThat(indexExists(connection, "uq_workflow_version_one_published")).isTrue();
            assertThat(foreignKeyExists(connection, "fk_agent_definition_current_draft_workflow")).isTrue();
            assertThat(foreignKeyExists(connection, "fk_trace_event_eval_run")).isTrue();
        }
    }

    /**
     * 验证物理外键和工作流版本唯一性约束实际生效。
     *
     * @throws SQLException 数据库访问失败时抛出
     */
    @Test
    void migrationEnforcesForeignKeysAndWorkflowVersionUniqueness() throws SQLException {
        migrate();

        try (Connection connection = openConnection()) {
            long schemaId = insertSchema(connection);
            long agentId = insertAgent(connection);
            long draftVersionId = insertWorkflowVersion(connection, agentId, 1, "DRAFT", null);
            updateAgentDraftWorkflow(connection, agentId, draftVersionId);

            // 同一个 Agent 只能存在一个当前 DRAFT 工作流版本。
            assertThatThrownBy(() -> insertWorkflowVersion(connection, agentId, 2, "DRAFT", null))
                    .isInstanceOf(SQLException.class);

            // Java 方法必须引用真实存在的输入输出 Schema。
            assertThatThrownBy(() -> insertJavaMethod(connection, schemaId, schemaId + 9999))
                    .isInstanceOf(SQLException.class);

            long publishedVersionId = insertWorkflowVersion(connection, agentId, 3, "PUBLISHED", draftVersionId);
            long runId = insertAgentRun(connection, agentId, publishedVersionId);
            long nodeRunId = insertNodeRun(connection, runId);
            long suiteId = insertEvalSuite(connection, agentId, publishedVersionId);
            long evalCaseId = insertEvalCase(connection, suiteId, runId, nodeRunId, publishedVersionId);
            long evalRunId = insertEvalRun(connection, suiteId, agentId, publishedVersionId, runId);
            insertEvalCaseResult(connection, evalRunId, evalCaseId);
            insertTraceEvent(connection, runId, nodeRunId, evalRunId);
        }
    }

    /**
     * 执行 Flyway 迁移。
     */
    private void migrate() {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .cleanDisabled(false)
                .load()
                .migrate();
    }

    /**
     * 打开 PostgreSQL 连接。
     *
     * @return 数据库连接
     * @throws SQLException 打开连接失败时抛出
     */
    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    /**
     * 判断表是否存在。
     *
     * @param connection 数据库连接
     * @param tableName 表名
     * @return 表存在时返回 true
     * @throws SQLException 查询失败时抛出
     */
    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                select count(*)
                from information_schema.tables
                where table_schema = 'public' and table_name = ?
                """)) {
            statement.setString(1, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) == 1;
            }
        }
    }

    /**
     * 判断索引是否存在。
     *
     * @param connection 数据库连接
     * @param indexName 索引名
     * @return 索引存在时返回 true
     * @throws SQLException 查询失败时抛出
     */
    private boolean indexExists(Connection connection, String indexName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                select count(*)
                from pg_indexes
                where schemaname = 'public' and indexname = ?
                """)) {
            statement.setString(1, indexName);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) == 1;
            }
        }
    }

    /**
     * 判断外键约束是否存在。
     *
     * @param connection 数据库连接
     * @param constraintName 外键约束名
     * @return 外键存在时返回 true
     * @throws SQLException 查询失败时抛出
     */
    private boolean foreignKeyExists(Connection connection, String constraintName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                select count(*)
                from information_schema.table_constraints
                where constraint_schema = 'public'
                  and constraint_type = 'FOREIGN KEY'
                  and constraint_name = ?
                """)) {
            statement.setString(1, constraintName);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) == 1;
            }
        }
    }

    /**
     * 插入 Schema 定义。
     *
     * @param connection 数据库连接
     * @return Schema 主键
     * @throws SQLException 插入失败时抛出
     */
    private long insertSchema(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into schema_definition(schema_key, version, name, json_schema, created_from, status)
                values ('test.input', 1, '测试输入', '{}'::jsonb, 'USER_CREATED', 'DRAFT')
                returning id
                """)) {
            return executeInsertReturningId(statement);
        }
    }

    /**
     * 插入 Agent 定义。
     *
     * @param connection 数据库连接
     * @return Agent 主键
     * @throws SQLException 插入失败时抛出
     */
    private long insertAgent(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into agent_definition(agent_key, name, status)
                values ('test-agent', '测试 Agent', 'ENABLED')
                returning id
                """)) {
            return executeInsertReturningId(statement);
        }
    }

    /**
     * 插入工作流版本。
     *
     * @param connection 数据库连接
     * @param agentId Agent 主键
     * @param versionNo 版本号
     * @param status 版本状态
     * @param sourceWorkflowVersionId 来源版本主键
     * @return 工作流版本主键
     * @throws SQLException 插入失败时抛出
     */
    private long insertWorkflowVersion(
            Connection connection,
            long agentId,
            int versionNo,
            String status,
            Long sourceWorkflowVersionId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into workflow_version(agent_id, version_no, status, source_workflow_version_id)
                values (?, ?, ?, ?)
                returning id
                """)) {
            statement.setLong(1, agentId);
            statement.setInt(2, versionNo);
            statement.setString(3, status);
            if (sourceWorkflowVersionId == null) {
                statement.setObject(4, null);
            } else {
                statement.setLong(4, sourceWorkflowVersionId);
            }
            return executeInsertReturningId(statement);
        }
    }

    /**
     * 更新 Agent 当前草稿指针。
     *
     * @param connection 数据库连接
     * @param agentId Agent 主键
     * @param workflowVersionId 工作流版本主键
     * @throws SQLException 更新失败时抛出
     */
    private void updateAgentDraftWorkflow(Connection connection, long agentId, long workflowVersionId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                update agent_definition
                set current_draft_workflow_version_id = ?
                where id = ?
                """)) {
            statement.setLong(1, workflowVersionId);
            statement.setLong(2, agentId);
            statement.executeUpdate();
        }
    }

    /**
     * 插入 Java 方法定义。
     *
     * @param connection 数据库连接
     * @param inputSchemaId 输入 Schema 主键
     * @param outputSchemaId 输出 Schema 主键
     * @return Java 方法主键
     * @throws SQLException 插入失败时抛出
     */
    private long insertJavaMethod(Connection connection, long inputSchemaId, long outputSchemaId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into java_method_definition(
                  method_key, name, bean_name, method_name, input_schema_id, output_schema_id, status
                )
                values ('test.method', '测试方法', 'testBean', 'run', ?, ?, 'ENABLED')
                returning id
                """)) {
            statement.setLong(1, inputSchemaId);
            statement.setLong(2, outputSchemaId);
            return executeInsertReturningId(statement);
        }
    }

    /**
     * 插入 AgentRun。
     *
     * @param connection 数据库连接
     * @param agentId Agent 主键
     * @param workflowVersionId 工作流版本主键
     * @return AgentRun 主键
     * @throws SQLException 插入失败时抛出
     */
    private long insertAgentRun(Connection connection, long agentId, long workflowVersionId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into agent_run(run_no, agent_id, agent_key, workflow_version_id, run_type, status)
                values ('RUN-0001', ?, 'test-agent', ?, 'DEBUG', 'SUCCESS')
                returning id
                """)) {
            statement.setLong(1, agentId);
            statement.setLong(2, workflowVersionId);
            return executeInsertReturningId(statement);
        }
    }

    /**
     * 插入 NodeRun。
     *
     * @param connection 数据库连接
     * @param runId AgentRun 主键
     * @return NodeRun 主键
     * @throws SQLException 插入失败时抛出
     */
    private long insertNodeRun(Connection connection, long runId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into node_run(run_id, node_id, node_name, node_type, status)
                values (?, 'llm_1', 'LLM 节点', 'LLM', 'SUCCESS')
                returning id
                """)) {
            statement.setLong(1, runId);
            return executeInsertReturningId(statement);
        }
    }

    /**
     * 插入 EvalSuite。
     *
     * @param connection 数据库连接
     * @param agentId Agent 主键
     * @param workflowVersionId 工作流版本主键
     * @return EvalSuite 主键
     * @throws SQLException 插入失败时抛出
     */
    private long insertEvalSuite(Connection connection, long agentId, long workflowVersionId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into eval_suite(agent_id, workflow_version_id, node_id, name, status)
                values (?, ?, 'llm_1', '测试验收套件', 'DRAFT')
                returning id
                """)) {
            statement.setLong(1, agentId);
            statement.setLong(2, workflowVersionId);
            return executeInsertReturningId(statement);
        }
    }

    /**
     * 插入 EvalCase。
     *
     * @param connection 数据库连接
     * @param suiteId EvalSuite 主键
     * @param runId AgentRun 主键
     * @param nodeRunId NodeRun 主键
     * @param workflowVersionId 工作流版本主键
     * @return EvalCase 主键
     * @throws SQLException 插入失败时抛出
     */
    private long insertEvalCase(
            Connection connection,
            long suiteId,
            long runId,
            long nodeRunId,
            long workflowVersionId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into eval_case(
                  suite_id, case_no, title, input_json, confirm_status,
                  source_agent_run_id, source_node_run_id, source_workflow_version_id, source_node_id
                )
                values (?, 'CASE-0001', '测试用例', '{}'::jsonb, 'USER_CREATED', ?, ?, ?, 'llm_1')
                returning id
                """)) {
            statement.setLong(1, suiteId);
            statement.setLong(2, runId);
            statement.setLong(3, nodeRunId);
            statement.setLong(4, workflowVersionId);
            return executeInsertReturningId(statement);
        }
    }

    /**
     * 插入 EvalRun。
     *
     * @param connection 数据库连接
     * @param suiteId EvalSuite 主键
     * @param agentId Agent 主键
     * @param workflowVersionId 工作流版本主键
     * @param agentRunId AgentRun 主键
     * @return EvalRun 主键
     * @throws SQLException 插入失败时抛出
     */
    private long insertEvalRun(
            Connection connection,
            long suiteId,
            long agentId,
            long workflowVersionId,
            long agentRunId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into eval_run(run_no, suite_id, agent_id, workflow_version_id, node_id, agent_run_id, status)
                values ('EVAL-0001', ?, ?, ?, 'llm_1', ?, 'SUCCESS')
                returning id
                """)) {
            statement.setLong(1, suiteId);
            statement.setLong(2, agentId);
            statement.setLong(3, workflowVersionId);
            statement.setLong(4, agentRunId);
            return executeInsertReturningId(statement);
        }
    }

    /**
     * 插入 EvalCaseResult。
     *
     * @param connection 数据库连接
     * @param evalRunId EvalRun 主键
     * @param evalCaseId EvalCase 主键
     * @throws SQLException 插入失败时抛出
     */
    private void insertEvalCaseResult(Connection connection, long evalRunId, long evalCaseId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into eval_case_result(eval_run_id, eval_case_id, passed)
                values (?, ?, true)
                """)) {
            statement.setLong(1, evalRunId);
            statement.setLong(2, evalCaseId);
            statement.executeUpdate();
        }
    }

    /**
     * 插入 TraceEvent。
     *
     * @param connection 数据库连接
     * @param runId AgentRun 主键
     * @param nodeRunId NodeRun 主键
     * @param evalRunId EvalRun 主键
     * @throws SQLException 插入失败时抛出
     */
    private void insertTraceEvent(Connection connection, long runId, long nodeRunId, long evalRunId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into trace_event(run_id, node_run_id, eval_run_id, event_type, summary)
                values (?, ?, ?, 'EVAL_CASE_RESULT', '验收用例通过')
                """)) {
            statement.setLong(1, runId);
            statement.setLong(2, nodeRunId);
            statement.setLong(3, evalRunId);
            statement.executeUpdate();
        }
    }

    /**
     * 执行插入语句并读取返回主键。
     *
     * @param statement 插入语句
     * @return 新增记录主键
     * @throws SQLException 插入失败时抛出
     */
    private long executeInsertReturningId(PreparedStatement statement) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }
}
