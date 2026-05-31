package com.myagent.run.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.myagent.common.repository.InstantTypeHandler;
import com.myagent.common.repository.JsonNodeTypeHandler;
import com.myagent.run.domain.TraceEventType;
import com.myagent.runtime.TraceEventRecord;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * TraceEvent MyBatis Mapper。
 */
@Mapper
public interface TraceEventMapper {

    /**
     * 插入 Trace 事件。
     *
     * @param record Trace 事件记录
     * @return 受影响行数
     */
    @Insert("""
            insert into trace_event(run_id, node_run_id, eval_run_id, event_type, summary, detail_json)
            values (
              #{record.agentRunDbId},
              #{record.nodeRunDbId},
              #{record.evalRunDbId},
              #{record.eventType},
              #{record.summary},
              #{record.detailJson, typeHandler=com.myagent.common.repository.JsonNodeTypeHandler}
            )
            """)
    int insert(@Param("record") TraceEventRecord record);

    /**
     * 查询最后插入的 Trace 事件。
     *
     * @param runId AgentRun 主键
     * @param nodeRunId NodeRun 主键
     * @param eventType 事件类型
     * @return Trace 事件
     */
    @ConstructorArgs({
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "run_id", javaType = Long.class),
            @Arg(column = "node_run_id", javaType = Long.class),
            @Arg(column = "eval_run_id", javaType = Long.class),
            @Arg(column = "event_type", javaType = TraceEventType.class),
            @Arg(column = "summary", javaType = String.class),
            @Arg(column = "detail_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "event_time", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class)
    })
    @Select({
            "<script>",
            "select id, run_id, node_run_id, eval_run_id, event_type, summary, detail_json, event_time",
            "from trace_event",
            "where event_type = #{eventType}",
            "  <if test='runId != null'> and run_id = #{runId} </if>",
            "  <if test='nodeRunId != null'> and node_run_id = #{nodeRunId} </if>",
            "order by id desc",
            "limit 1",
            "</script>"
    })
    RunTraceEventRecord findLatest(
            @Param("runId") Long runId,
            @Param("nodeRunId") Long nodeRunId,
            @Param("eventType") TraceEventType eventType
    );

    /**
     * 查询运行下的 Trace 事件。
     *
     * @param runId AgentRun 主键
     * @return Trace 事件列表
     */
    @ConstructorArgs({
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "run_id", javaType = Long.class),
            @Arg(column = "node_run_id", javaType = Long.class),
            @Arg(column = "eval_run_id", javaType = Long.class),
            @Arg(column = "event_type", javaType = TraceEventType.class),
            @Arg(column = "summary", javaType = String.class),
            @Arg(column = "detail_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "event_time", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class)
    })
    @Select("""
            select id, run_id, node_run_id, eval_run_id, event_type, summary, detail_json, event_time
            from trace_event
            where run_id = #{runId}
            order by event_time asc, id asc
            """)
    List<RunTraceEventRecord> listByRunId(@Param("runId") long runId);
}
