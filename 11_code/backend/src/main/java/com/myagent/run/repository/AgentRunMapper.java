package com.myagent.run.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.myagent.common.repository.InstantTypeHandler;
import com.myagent.common.repository.JsonNodeTypeHandler;
import com.myagent.run.domain.RunStatus;
import com.myagent.run.domain.RunType;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * AgentRun MyBatis Mapper。
 */
@Mapper
public interface AgentRunMapper {

    /**
     * 插入运行记录。
     *
     * @param record 运行记录
     * @return 受影响行数
     */
    @Insert("""
            insert into agent_run(
              run_no, agent_id, agent_key, workflow_version_id, parent_run_id, run_type,
              input_json, output_json, status, error_code, error_message
            )
            values (
              #{record.runNo},
              #{record.agentId},
              #{record.agentKey},
              #{record.workflowVersionId},
              #{record.parentRunId},
              #{record.runType},
              #{record.inputJson, typeHandler=com.myagent.common.repository.JsonNodeTypeHandler},
              #{record.outputJson, typeHandler=com.myagent.common.repository.JsonNodeTypeHandler},
              #{record.status},
              #{record.errorCode},
              #{record.errorMessage}
            )
            """)
    int insert(@Param("record") AgentRunRecord record);

    /**
     * 按运行编号查询。
     *
     * @param runNo 运行编号
     * @return 运行记录
     */
    @ConstructorArgs({
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "run_no", javaType = String.class),
            @Arg(column = "agent_id", javaType = long.class),
            @Arg(column = "agent_key", javaType = String.class),
            @Arg(column = "workflow_version_id", javaType = long.class),
            @Arg(column = "parent_run_id", javaType = Long.class),
            @Arg(column = "run_type", javaType = RunType.class),
            @Arg(column = "input_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "output_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "status", javaType = RunStatus.class),
            @Arg(column = "error_code", javaType = String.class),
            @Arg(column = "error_message", javaType = String.class),
            @Arg(column = "started_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "finished_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "duration_ms", javaType = Long.class)
    })
    @Select("""
            select id, run_no, agent_id, agent_key, workflow_version_id, parent_run_id, run_type,
                   input_json, output_json, status, error_code, error_message, started_at, finished_at, duration_ms
            from agent_run
            where run_no = #{runNo}
            """)
    AgentRunRecord findByRunNo(@Param("runNo") String runNo);

    /**
     * 按主键查询。
     *
     * @param runId 运行主键
     * @return 运行记录
     */
    @ConstructorArgs({
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "run_no", javaType = String.class),
            @Arg(column = "agent_id", javaType = long.class),
            @Arg(column = "agent_key", javaType = String.class),
            @Arg(column = "workflow_version_id", javaType = long.class),
            @Arg(column = "parent_run_id", javaType = Long.class),
            @Arg(column = "run_type", javaType = RunType.class),
            @Arg(column = "input_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "output_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "status", javaType = RunStatus.class),
            @Arg(column = "error_code", javaType = String.class),
            @Arg(column = "error_message", javaType = String.class),
            @Arg(column = "started_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "finished_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "duration_ms", javaType = Long.class)
    })
    @Select("""
            select id, run_no, agent_id, agent_key, workflow_version_id, parent_run_id, run_type,
                   input_json, output_json, status, error_code, error_message, started_at, finished_at, duration_ms
            from agent_run
            where id = #{runId}
            """)
    AgentRunRecord findById(@Param("runId") long runId);

    /**
     * 分页查询运行记录。
     *
     * @param agentId Agent 主键
     * @param agentKey Agent 业务标识
     * @param runType 运行类型
     * @param status 运行状态
     * @param keyword 关键词
     * @param startedAtFrom 开始时间下界
     * @param startedAtTo 开始时间上界
     * @param limit 限制条数
     * @param offset 偏移量
     * @return 运行记录列表
     */
    @ConstructorArgs({
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "run_no", javaType = String.class),
            @Arg(column = "agent_id", javaType = long.class),
            @Arg(column = "agent_key", javaType = String.class),
            @Arg(column = "workflow_version_id", javaType = long.class),
            @Arg(column = "parent_run_id", javaType = Long.class),
            @Arg(column = "run_type", javaType = RunType.class),
            @Arg(column = "input_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "output_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "status", javaType = RunStatus.class),
            @Arg(column = "error_code", javaType = String.class),
            @Arg(column = "error_message", javaType = String.class),
            @Arg(column = "started_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "finished_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "duration_ms", javaType = Long.class)
    })
    @Select({
            "<script>",
            "select id, run_no, agent_id, agent_key, workflow_version_id, parent_run_id, run_type,",
            "       input_json, output_json, status, error_code, error_message, started_at, finished_at, duration_ms",
            "from agent_run",
            "<where>",
            "  <if test='agentId != null'> and agent_id = #{agentId} </if>",
            "  <if test='agentKey != null and agentKey != \"\"'> and agent_key = #{agentKey} </if>",
            "  <choose>",
            "    <when test='runType != null'> and run_type = #{runType} </when>",
            "    <otherwise> and run_type in ('API', 'DEBUG', 'AGENT_CALL') </otherwise>",
            "  </choose>",
            "  <if test='status != null'> and status = #{status} </if>",
            "  <if test='keyword != null and keyword != \"\"'>",
            "    and (lower(run_no) like concat('%', lower(#{keyword}), '%')",
            "         or lower(agent_key) like concat('%', lower(#{keyword}), '%')",
            "         or lower(error_message) like concat('%', lower(#{keyword}), '%'))",
            "  </if>",
            "  <if test='startedAtFrom != null'> and started_at &gt;= #{startedAtFrom} </if>",
            "  <if test='startedAtTo != null'> and started_at &lt;= #{startedAtTo} </if>",
            "</where>",
            "order by started_at desc, id desc",
            "limit #{limit} offset #{offset}",
            "</script>"
    })
    List<AgentRunRecord> listRuns(
            @Param("agentId") Long agentId,
            @Param("agentKey") String agentKey,
            @Param("runType") RunType runType,
            @Param("status") RunStatus status,
            @Param("keyword") String keyword,
            @Param("startedAtFrom") java.time.Instant startedAtFrom,
            @Param("startedAtTo") java.time.Instant startedAtTo,
            @Param("limit") long limit,
            @Param("offset") long offset
    );

    /**
     * 统计运行记录数量。
     *
     * @param agentId Agent 主键
     * @param agentKey Agent 业务标识
     * @param runType 运行类型
     * @param status 运行状态
     * @param keyword 关键词
     * @param startedAtFrom 开始时间下界
     * @param startedAtTo 开始时间上界
     * @return 总数
     */
    @Select({
            "<script>",
            "select count(*) from agent_run",
            "<where>",
            "  <if test='agentId != null'> and agent_id = #{agentId} </if>",
            "  <if test='agentKey != null and agentKey != \"\"'> and agent_key = #{agentKey} </if>",
            "  <choose>",
            "    <when test='runType != null'> and run_type = #{runType} </when>",
            "    <otherwise> and run_type in ('API', 'DEBUG', 'AGENT_CALL') </otherwise>",
            "  </choose>",
            "  <if test='status != null'> and status = #{status} </if>",
            "  <if test='keyword != null and keyword != \"\"'>",
            "    and (lower(run_no) like concat('%', lower(#{keyword}), '%')",
            "         or lower(agent_key) like concat('%', lower(#{keyword}), '%')",
            "         or lower(error_message) like concat('%', lower(#{keyword}), '%'))",
            "  </if>",
            "  <if test='startedAtFrom != null'> and started_at &gt;= #{startedAtFrom} </if>",
            "  <if test='startedAtTo != null'> and started_at &lt;= #{startedAtTo} </if>",
            "</where>",
            "</script>"
    })
    long countRuns(
            @Param("agentId") Long agentId,
            @Param("agentKey") String agentKey,
            @Param("runType") RunType runType,
            @Param("status") RunStatus status,
            @Param("keyword") String keyword,
            @Param("startedAtFrom") java.time.Instant startedAtFrom,
            @Param("startedAtTo") java.time.Instant startedAtTo
    );

    /**
     * 标记运行为运行中。
     *
     * @param runId 运行主键
     * @return 受影响行数
     */
    @Update("""
            update agent_run
            set status = 'RUNNING'
            where id = #{runId}
            """)
    int markRunning(@Param("runId") long runId);

    /**
     * 完成运行。
     *
     * @param runId 运行主键
     * @param status 运行状态
     * @param outputJson 输出 JSON
     * @param errorCode 错误码
     * @param errorMessage 错误消息
     * @param durationMs 耗时毫秒
     * @return 受影响行数
     */
    @Update("""
            update agent_run
            set status = #{status},
                output_json = #{outputJson, typeHandler=com.myagent.common.repository.JsonNodeTypeHandler},
                error_code = #{errorCode},
                error_message = #{errorMessage},
                finished_at = now(),
                duration_ms = #{durationMs}
            where id = #{runId}
            """)
    int finishRun(
            @Param("runId") long runId,
            @Param("status") RunStatus status,
            @Param("outputJson") JsonNode outputJson,
            @Param("errorCode") String errorCode,
            @Param("errorMessage") String errorMessage,
            @Param("durationMs") long durationMs
    );

    /**
     * 取消尚未完成的运行。
     *
     * @param runId 运行主键
     * @param errorCode 错误码
     * @param errorMessage 错误消息
     * @param durationMs 耗时毫秒
     * @return 受影响行数
     */
    @Update("""
            update agent_run
            set status = 'CANCELED',
                error_code = #{errorCode},
                error_message = #{errorMessage},
                finished_at = now(),
                duration_ms = #{durationMs}
            where id = #{runId}
              and status in ('PENDING', 'RUNNING')
            """)
    int cancelActiveRun(
            @Param("runId") long runId,
            @Param("errorCode") String errorCode,
            @Param("errorMessage") String errorMessage,
            @Param("durationMs") long durationMs
    );
}
