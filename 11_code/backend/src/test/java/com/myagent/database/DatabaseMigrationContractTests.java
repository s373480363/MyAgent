package com.myagent.database;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 数据库迁移脚本的静态契约测试。
 */
class DatabaseMigrationContractTests {

    /**
     * 核心迁移脚本路径。
     */
    private static final Path MIGRATION_PATH = Path.of(
            "src", "main", "resources", "db", "migration", "V1__create_core_tables.sql"
    );

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
     * 验证迁移脚本完整创建 V1 核心表。
     *
     * @throws IOException 读取迁移脚本失败时抛出
     */
    @Test
    void migrationDefinesAllCoreTables() throws IOException {
        String sql = readMigrationSql();

        // 逐表检查可以尽早发现新增表遗漏或迁移文件被误删的问题。
        for (String tableName : CORE_TABLES) {
            assertThat(sql).contains("create table " + tableName + " (");
        }
    }

    /**
     * 验证迁移脚本包含核心业务唯一约束。
     *
     * @throws IOException 读取迁移脚本失败时抛出
     */
    @Test
    void migrationDefinesBusinessKeysAndWorkflowVersionGuards() throws IOException {
        String sql = readMigrationSql();

        // 这些约束是后续主数据、版本和运行回溯的稳定业务键。
        assertThat(sql)
                .contains("constraint uq_schema_definition_key_version unique (schema_key, version)")
                .contains("constraint uq_java_method_definition_method_key unique (method_key)")
                .contains("constraint uq_tool_definition_tool_key unique (tool_key)")
                .contains("constraint uq_external_agent_definition_adapter_key unique (adapter_key)")
                .contains("constraint uq_agent_definition_agent_key unique (agent_key)")
                .contains("constraint uq_workflow_version_agent_version unique (agent_id, version_no)")
                .contains("constraint uq_agent_run_run_no unique (run_no)")
                .contains("constraint uq_eval_run_run_no unique (run_no)")
                .contains("create unique index uq_workflow_version_one_draft")
                .contains("where status = 'draft'")
                .contains("create unique index uq_workflow_version_one_published")
                .contains("where status = 'published'");
    }

    /**
     * 验证迁移脚本显式使用物理外键。
     *
     * @throws IOException 读取迁移脚本失败时抛出
     */
    @Test
    void migrationDefinesPhysicalForeignKeys() throws IOException {
        String sql = readMigrationSql();

        // 用户已确认使用物理外键，这里检查跨核心对象的关键外键都已落库。
        assertThat(sql)
                .contains("constraint fk_workflow_version_agent foreign key (agent_id) references agent_definition(id)")
                .contains("constraint fk_workflow_version_source foreign key (source_workflow_version_id) references workflow_version(id)")
                .contains("constraint fk_agent_definition_current_draft_workflow")
                .contains("constraint fk_agent_definition_current_published_workflow")
                .contains("constraint fk_java_method_definition_input_schema foreign key (input_schema_id) references schema_definition(id)")
                .contains("constraint fk_tool_definition_output_schema foreign key (output_schema_id) references schema_definition(id)")
                .contains("constraint fk_external_agent_definition_output_schema foreign key (output_schema_id) references schema_definition(id)")
                .contains("constraint fk_agent_run_workflow_version foreign key (workflow_version_id) references workflow_version(id)")
                .contains("constraint fk_node_run_agent_run foreign key (run_id) references agent_run(id)")
                .contains("constraint fk_trace_event_node_run foreign key (node_run_id) references node_run(id)")
                .contains("constraint fk_trace_event_eval_run foreign key (eval_run_id) references eval_run(id)")
                .contains("constraint fk_eval_case_source_node_run foreign key (source_node_run_id) references node_run(id)")
                .contains("constraint fk_eval_case_result_eval_case foreign key (eval_case_id) references eval_case(id)");
    }

    /**
     * 验证运行态 ID 语义没有被迁移脚本引入第二套编号。
     *
     * @throws IOException 读取迁移脚本失败时抛出
     */
    @Test
    void migrationKeepsRuntimeIdSemanticsStable() throws IOException {
        String sql = readMigrationSql();
        String nodeRunTable = extractCreateTableBlock(sql, "node_run");

        // run_no 只存在于 AgentRun 和 EvalRun，NodeRun 不允许派生第二套对外编号。
        assertThat(sql).contains("run_no varchar(64) not null");
        assertThat(nodeRunTable)
                .doesNotContain("run_no varchar")
                .doesNotContain("node_run_no");
    }

    /**
     * 读取核心迁移脚本。
     *
     * @return 迁移脚本文本
     * @throws IOException 读取失败时抛出
     */
    private String readMigrationSql() throws IOException {
        return Files.readString(MIGRATION_PATH, StandardCharsets.UTF_8).toLowerCase();
    }

    /**
     * 提取单个建表语句块。
     *
     * @param sql 完整迁移脚本
     * @param tableName 表名
     * @return 建表语句块
     */
    private String extractCreateTableBlock(String sql, String tableName) {
        String startMarker = "create table " + tableName + " (";
        int start = sql.indexOf(startMarker);
        assertThat(start).isGreaterThanOrEqualTo(0);
        int end = sql.indexOf("\n);", start);
        assertThat(end).isGreaterThan(start);
        return sql.substring(start, end);
    }
}
