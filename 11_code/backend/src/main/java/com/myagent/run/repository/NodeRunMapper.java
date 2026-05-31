package com.myagent.run.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.myagent.common.repository.InstantTypeHandler;
import com.myagent.common.repository.JsonNodeTypeHandler;
import com.myagent.run.domain.RunStatus;
import com.myagent.runtime.NodeRunFinishRecord;
import com.myagent.runtime.NodeRunStartRecord;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * NodeRun MyBatis Mapper。
 */
@Mapper
public interface NodeRunMapper {

    /**
     * 插入节点运行记录。
     *
     * @param record 节点运行开始记录
     * @return 受影响行数
     */
    @Insert("""
            insert into node_run(
              run_id, node_id, node_name, node_type, input_json, status, error_message
            )
            values (
              #{record.agentRunDbId},
              #{record.nodeId},
              #{record.nodeName},
              #{record.nodeType},
              #{record.inputJson, typeHandler=com.myagent.common.repository.JsonNodeTypeHandler},
              'RUNNING',
              ''
            )
            """)
    int insert(@Param("record") NodeRunStartRecord record);

    /**
     * 查询最后插入的节点运行记录。
     *
     * @param runId AgentRun 主键
     * @param nodeId 节点标识
     * @return 节点运行记录
     */
    @ConstructorArgs({
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "run_id", javaType = long.class),
            @Arg(column = "node_id", javaType = String.class),
            @Arg(column = "node_name", javaType = String.class),
            @Arg(column = "node_type", javaType = String.class),
            @Arg(column = "input_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "output_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "schema_validation_result_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "status", javaType = RunStatus.class),
            @Arg(column = "error_message", javaType = String.class),
            @Arg(column = "started_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "finished_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "duration_ms", javaType = Long.class)
    })
    @Select("""
            select id, run_id, node_id, node_name, node_type, input_json, output_json,
                   schema_validation_result_json, status, error_message, started_at, finished_at, duration_ms
            from node_run
            where run_id = #{runId} and node_id = #{nodeId}
            order by id desc
            limit 1
            """)
    NodeRunRecord findLatestByRunAndNode(@Param("runId") long runId, @Param("nodeId") String nodeId);

    /**
     * 按主键查询。
     *
     * @param nodeRunId NodeRun 主键
     * @return 节点运行记录
     */
    @ConstructorArgs({
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "run_id", javaType = long.class),
            @Arg(column = "node_id", javaType = String.class),
            @Arg(column = "node_name", javaType = String.class),
            @Arg(column = "node_type", javaType = String.class),
            @Arg(column = "input_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "output_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "schema_validation_result_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "status", javaType = RunStatus.class),
            @Arg(column = "error_message", javaType = String.class),
            @Arg(column = "started_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "finished_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "duration_ms", javaType = Long.class)
    })
    @Select("""
            select id, run_id, node_id, node_name, node_type, input_json, output_json,
                   schema_validation_result_json, status, error_message, started_at, finished_at, duration_ms
            from node_run
            where id = #{nodeRunId}
            """)
    NodeRunRecord findById(@Param("nodeRunId") long nodeRunId);

    /**
     * 查询运行下节点记录。
     *
     * @param runId AgentRun 主键
     * @return 节点运行记录列表
     */
    @ConstructorArgs({
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "run_id", javaType = long.class),
            @Arg(column = "node_id", javaType = String.class),
            @Arg(column = "node_name", javaType = String.class),
            @Arg(column = "node_type", javaType = String.class),
            @Arg(column = "input_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "output_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "schema_validation_result_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "status", javaType = RunStatus.class),
            @Arg(column = "error_message", javaType = String.class),
            @Arg(column = "started_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "finished_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "duration_ms", javaType = Long.class)
    })
    @Select("""
            select id, run_id, node_id, node_name, node_type, input_json, output_json,
                   schema_validation_result_json, status, error_message, started_at, finished_at, duration_ms
            from node_run
            where run_id = #{runId}
            order by started_at asc, id asc
            """)
    List<NodeRunRecord> listByRunId(@Param("runId") long runId);

    /**
     * 完成节点运行。
     *
     * @param record 节点运行完成记录
     * @return 受影响行数
     */
    @Update("""
            update node_run
            set status = #{record.status},
                output_json = #{record.outputJson, typeHandler=com.myagent.common.repository.JsonNodeTypeHandler},
                schema_validation_result_json = #{record.schemaValidationResultJson, typeHandler=com.myagent.common.repository.JsonNodeTypeHandler},
                error_message = #{record.errorMessage},
                finished_at = now(),
                duration_ms = #{record.durationMs}
            where id = #{record.nodeRunDbId}
            """)
    int finish(@Param("record") NodeRunFinishRecord record);
}
