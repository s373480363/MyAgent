package com.myagent.externalagent.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.myagent.common.domain.EnableStatus;
import com.myagent.common.repository.InstantTypeHandler;
import com.myagent.common.repository.JsonNodeTypeHandler;
import com.myagent.externalagent.domain.ExternalAgentType;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 外部 Agent MyBatis Mapper。
 */
@Mapper
public interface ExternalAgentMapper {

    /**
     * 分页查询外部 Agent。
     *
     * @param keyword 关键词
     * @param status 状态
     * @param adapterType 类型
     * @param limit 限制条数
     * @param offset 偏移量
     * @return 记录列表
     */
    @ConstructorArgs({
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "adapter_key", javaType = String.class),
            @Arg(column = "adapter_type", javaType = ExternalAgentType.class),
            @Arg(column = "name", javaType = String.class),
            @Arg(column = "description", javaType = String.class),
            @Arg(column = "command_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "working_directory", javaType = String.class),
            @Arg(column = "timeout_seconds", javaType = int.class),
            @Arg(column = "capture_stdout", javaType = boolean.class),
            @Arg(column = "capture_stderr", javaType = boolean.class),
            @Arg(column = "capture_git_diff", javaType = boolean.class),
            @Arg(column = "output_schema_id", javaType = Long.class),
            @Arg(column = "status", javaType = EnableStatus.class),
            @Arg(column = "created_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "updated_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class)
    })
    @Select({
            "<script>",
            "select id, adapter_key, adapter_type, name, description, command_json, working_directory,",
            "       timeout_seconds, capture_stdout, capture_stderr, capture_git_diff, output_schema_id,",
            "       status, created_at, updated_at",
            "from external_agent_definition",
            "<where>",
            "  <if test='keyword != null and keyword != \"\"'>",
            "    and (",
            "      lower(adapter_key) like concat('%', lower(#{keyword}), '%')",
            "      or lower(name) like concat('%', lower(#{keyword}), '%')",
            "      or lower(description) like concat('%', lower(#{keyword}), '%')",
            "    )",
            "  </if>",
            "  <if test='status != null'>",
            "    and status = #{status}",
            "  </if>",
            "  <if test='adapterType != null'>",
            "    and adapter_type = #{adapterType}",
            "  </if>",
            "</where>",
            "order by updated_at desc, id desc",
            "limit #{limit} offset #{offset}",
            "</script>"
    })
    List<ExternalAgentRecord> listExternalAgents(
            @Param("keyword") String keyword,
            @Param("status") EnableStatus status,
            @Param("adapterType") ExternalAgentType adapterType,
            @Param("limit") long limit,
            @Param("offset") long offset
    );

    /**
     * 统计外部 Agent 数量。
     *
     * @param keyword 关键词
     * @param status 状态
     * @param adapterType 类型
     * @return 总数
     */
    @Select({
            "<script>",
            "select count(*)",
            "from external_agent_definition",
            "<where>",
            "  <if test='keyword != null and keyword != \"\"'>",
            "    and (",
            "      lower(adapter_key) like concat('%', lower(#{keyword}), '%')",
            "      or lower(name) like concat('%', lower(#{keyword}), '%')",
            "      or lower(description) like concat('%', lower(#{keyword}), '%')",
            "    )",
            "  </if>",
            "  <if test='status != null'>",
            "    and status = #{status}",
            "  </if>",
            "  <if test='adapterType != null'>",
            "    and adapter_type = #{adapterType}",
            "  </if>",
            "</where>",
            "</script>"
    })
    long countExternalAgents(
            @Param("keyword") String keyword,
            @Param("status") EnableStatus status,
            @Param("adapterType") ExternalAgentType adapterType
    );

    /**
     * 按主键查询外部 Agent。
     *
     * @param adapterId 主键
     * @return 外部 Agent 记录
     */
    @ConstructorArgs({
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "adapter_key", javaType = String.class),
            @Arg(column = "adapter_type", javaType = ExternalAgentType.class),
            @Arg(column = "name", javaType = String.class),
            @Arg(column = "description", javaType = String.class),
            @Arg(column = "command_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "working_directory", javaType = String.class),
            @Arg(column = "timeout_seconds", javaType = int.class),
            @Arg(column = "capture_stdout", javaType = boolean.class),
            @Arg(column = "capture_stderr", javaType = boolean.class),
            @Arg(column = "capture_git_diff", javaType = boolean.class),
            @Arg(column = "output_schema_id", javaType = Long.class),
            @Arg(column = "status", javaType = EnableStatus.class),
            @Arg(column = "created_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "updated_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class)
    })
    @Select("""
            select id, adapter_key, adapter_type, name, description, command_json, working_directory,
                   timeout_seconds, capture_stdout, capture_stderr, capture_git_diff, output_schema_id,
                   status, created_at, updated_at
            from external_agent_definition
            where id = #{adapterId}
            """)
    ExternalAgentRecord findById(@Param("adapterId") long adapterId);

    /**
     * 按适配器标识查询外部 Agent。
     *
     * @param adapterKey 适配器标识
     * @return 外部 Agent 记录
     */
    @ConstructorArgs({
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "adapter_key", javaType = String.class),
            @Arg(column = "adapter_type", javaType = ExternalAgentType.class),
            @Arg(column = "name", javaType = String.class),
            @Arg(column = "description", javaType = String.class),
            @Arg(column = "command_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "working_directory", javaType = String.class),
            @Arg(column = "timeout_seconds", javaType = int.class),
            @Arg(column = "capture_stdout", javaType = boolean.class),
            @Arg(column = "capture_stderr", javaType = boolean.class),
            @Arg(column = "capture_git_diff", javaType = boolean.class),
            @Arg(column = "output_schema_id", javaType = Long.class),
            @Arg(column = "status", javaType = EnableStatus.class),
            @Arg(column = "created_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "updated_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class)
    })
    @Select("""
            select id, adapter_key, adapter_type, name, description, command_json, working_directory,
                   timeout_seconds, capture_stdout, capture_stderr, capture_git_diff, output_schema_id,
                   status, created_at, updated_at
            from external_agent_definition
            where adapter_key = #{adapterKey}
            """)
    ExternalAgentRecord findByAdapterKey(@Param("adapterKey") String adapterKey);

    /**
     * 插入外部 Agent，并返回完整记录。
     *
     * @param record 外部 Agent 记录
     * @return 新增后的记录
     */
    @ConstructorArgs({
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "adapter_key", javaType = String.class),
            @Arg(column = "adapter_type", javaType = ExternalAgentType.class),
            @Arg(column = "name", javaType = String.class),
            @Arg(column = "description", javaType = String.class),
            @Arg(column = "command_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "working_directory", javaType = String.class),
            @Arg(column = "timeout_seconds", javaType = int.class),
            @Arg(column = "capture_stdout", javaType = boolean.class),
            @Arg(column = "capture_stderr", javaType = boolean.class),
            @Arg(column = "capture_git_diff", javaType = boolean.class),
            @Arg(column = "output_schema_id", javaType = Long.class),
            @Arg(column = "status", javaType = EnableStatus.class),
            @Arg(column = "created_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "updated_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class)
    })
    @Select("""
            insert into external_agent_definition(
              adapter_key, adapter_type, name, description, command_json, working_directory,
              timeout_seconds, capture_stdout, capture_stderr, capture_git_diff, output_schema_id, status
            )
            values (
              #{record.adapterKey},
              #{record.adapterType},
              #{record.name},
              #{record.description},
              #{record.commandJson, typeHandler=com.myagent.common.repository.JsonNodeTypeHandler},
              #{record.workingDirectory},
              #{record.timeoutSeconds},
              #{record.captureStdout},
              #{record.captureStderr},
              #{record.captureGitDiff},
              #{record.outputSchemaId},
              #{record.status}
            )
            returning id, adapter_key, adapter_type, name, description, command_json, working_directory,
                      timeout_seconds, capture_stdout, capture_stderr, capture_git_diff, output_schema_id,
                      status, created_at, updated_at
            """)
    ExternalAgentRecord insert(@Param("record") ExternalAgentRecord record);

    /**
     * 更新外部 Agent。
     *
     * @param record 外部 Agent 记录
     * @return 受影响行数
     */
    @Update("""
            update external_agent_definition
            set name = #{record.name},
                description = #{record.description},
                command_json = #{record.commandJson, typeHandler=com.myagent.common.repository.JsonNodeTypeHandler},
                working_directory = #{record.workingDirectory},
                timeout_seconds = #{record.timeoutSeconds},
                capture_stdout = #{record.captureStdout},
                capture_stderr = #{record.captureStderr},
                capture_git_diff = #{record.captureGitDiff},
                output_schema_id = #{record.outputSchemaId},
                status = #{record.status},
                updated_at = now()
            where id = #{record.id}
            """)
    int update(@Param("record") ExternalAgentRecord record);

    /**
     * 仅更新外部 Agent 状态。
     *
     * @param adapterId 外部 Agent 主键
     * @param status 新状态
     * @return 受影响行数
     */
    @Update("""
            update external_agent_definition
            set status = #{status},
                updated_at = now()
            where id = #{adapterId}
            """)
    int updateStatus(@Param("adapterId") long adapterId, @Param("status") EnableStatus status);
}
