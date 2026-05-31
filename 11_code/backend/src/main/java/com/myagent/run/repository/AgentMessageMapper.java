package com.myagent.run.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.myagent.common.repository.InstantTypeHandler;
import com.myagent.common.repository.JsonNodeTypeHandler;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * AgentMessage MyBatis Mapper。
 */
@Mapper
public interface AgentMessageMapper {

    /**
     * 插入 Agent 消息。
     *
     * @param record Agent 消息记录
     * @return 受影响行数
     */
    @Insert("""
            insert into agent_message(
              parent_run_id, child_run_id, source_agent_id, target_agent_id, input_json, output_json, summary
            )
            values (
              #{record.parentRunId},
              #{record.childRunId},
              #{record.sourceAgentId},
              #{record.targetAgentId},
              #{record.inputJson, typeHandler=com.myagent.common.repository.JsonNodeTypeHandler},
              #{record.outputJson, typeHandler=com.myagent.common.repository.JsonNodeTypeHandler},
              #{record.summary}
            )
            """)
    int insert(@Param("record") AgentMessageRecord record);

    /**
     * 按子运行查询 Agent 消息。
     *
     * @param childRunId 子运行主键
     * @return Agent 消息
     */
    @ConstructorArgs({
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "parent_run_id", javaType = long.class),
            @Arg(column = "child_run_id", javaType = long.class),
            @Arg(column = "source_agent_id", javaType = long.class),
            @Arg(column = "target_agent_id", javaType = long.class),
            @Arg(column = "input_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "output_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "summary", javaType = String.class),
            @Arg(column = "created_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class)
    })
    @Select("""
            select id, parent_run_id, child_run_id, source_agent_id, target_agent_id,
                   input_json, output_json, summary, created_at
            from agent_message
            where child_run_id = #{childRunId}
            """)
    AgentMessageRecord findByChildRunId(@Param("childRunId") long childRunId);

    /**
     * 查询父运行下的 Agent 消息。
     *
     * @param parentRunId 父运行主键
     * @return Agent 消息列表
     */
    @ConstructorArgs({
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "parent_run_id", javaType = long.class),
            @Arg(column = "child_run_id", javaType = long.class),
            @Arg(column = "source_agent_id", javaType = long.class),
            @Arg(column = "target_agent_id", javaType = long.class),
            @Arg(column = "input_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "output_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "summary", javaType = String.class),
            @Arg(column = "created_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class)
    })
    @Select("""
            select id, parent_run_id, child_run_id, source_agent_id, target_agent_id,
                   input_json, output_json, summary, created_at
            from agent_message
            where parent_run_id = #{parentRunId}
            order by created_at asc, id asc
            """)
    List<AgentMessageRecord> listByParentRunId(@Param("parentRunId") long parentRunId);
}
