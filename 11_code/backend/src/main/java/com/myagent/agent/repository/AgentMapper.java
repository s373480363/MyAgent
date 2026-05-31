package com.myagent.agent.repository;

import com.myagent.common.domain.EnableStatus;
import com.myagent.common.repository.InstantTypeHandler;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;

/**
 * Agent MyBatis Mapper。
 */
@Mapper
public interface AgentMapper {

    /**
     * 分页查询 Agent。
     *
     * @param keyword 关键词
     * @param status 状态
     * @param limit 限制条数
     * @param offset 偏移量
     * @return Agent 记录列表
     */
    @ConstructorArgs({
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "agent_key", javaType = String.class),
            @Arg(column = "name", javaType = String.class),
            @Arg(column = "description", javaType = String.class),
            @Arg(column = "status", javaType = EnableStatus.class),
            @Arg(column = "system_prompt", javaType = String.class),
            @Arg(column = "default_model", javaType = String.class),
            @Arg(column = "temperature", javaType = BigDecimal.class),
            @Arg(column = "timeout_seconds", javaType = int.class),
            @Arg(column = "max_steps", javaType = int.class),
            @Arg(column = "current_draft_workflow_version_id", javaType = Long.class),
            @Arg(column = "current_published_workflow_version_id", javaType = Long.class),
            @Arg(column = "created_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "updated_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class)
    })
    @Select({
            "<script>",
            "select id, agent_key, name, description, status, system_prompt, default_model, temperature,",
            "       timeout_seconds, max_steps, current_draft_workflow_version_id,",
            "       current_published_workflow_version_id, created_at, updated_at",
            "from agent_definition",
            "<where>",
            "  <if test='keyword != null and keyword != \"\"'>",
            "    and (",
            "      lower(agent_key) like concat('%', lower(#{keyword}), '%')",
            "      or lower(name) like concat('%', lower(#{keyword}), '%')",
            "      or lower(description) like concat('%', lower(#{keyword}), '%')",
            "    )",
            "  </if>",
            "  <if test='status != null'>",
            "    and status = #{status}",
            "  </if>",
            "</where>",
            "order by updated_at desc, id desc",
            "limit #{limit} offset #{offset}",
            "</script>"
    })
    List<AgentRecord> listAgents(
            @Param("keyword") String keyword,
            @Param("status") EnableStatus status,
            @Param("limit") long limit,
            @Param("offset") long offset
    );

    /**
     * 统计 Agent 数量。
     *
     * @param keyword 关键词
     * @param status 状态
     * @return 总数
     */
    @Select({
            "<script>",
            "select count(*)",
            "from agent_definition",
            "<where>",
            "  <if test='keyword != null and keyword != \"\"'>",
            "    and (",
            "      lower(agent_key) like concat('%', lower(#{keyword}), '%')",
            "      or lower(name) like concat('%', lower(#{keyword}), '%')",
            "      or lower(description) like concat('%', lower(#{keyword}), '%')",
            "    )",
            "  </if>",
            "  <if test='status != null'>",
            "    and status = #{status}",
            "  </if>",
            "</where>",
            "</script>"
    })
    long countAgents(@Param("keyword") String keyword, @Param("status") EnableStatus status);

    /**
     * 按主键查询 Agent。
     *
     * @param agentId Agent 主键
     * @return Agent 记录
     */
    @ConstructorArgs({
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "agent_key", javaType = String.class),
            @Arg(column = "name", javaType = String.class),
            @Arg(column = "description", javaType = String.class),
            @Arg(column = "status", javaType = EnableStatus.class),
            @Arg(column = "system_prompt", javaType = String.class),
            @Arg(column = "default_model", javaType = String.class),
            @Arg(column = "temperature", javaType = BigDecimal.class),
            @Arg(column = "timeout_seconds", javaType = int.class),
            @Arg(column = "max_steps", javaType = int.class),
            @Arg(column = "current_draft_workflow_version_id", javaType = Long.class),
            @Arg(column = "current_published_workflow_version_id", javaType = Long.class),
            @Arg(column = "created_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "updated_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class)
    })
    @Select("""
            select id, agent_key, name, description, status, system_prompt, default_model, temperature,
                   timeout_seconds, max_steps, current_draft_workflow_version_id,
                   current_published_workflow_version_id, created_at, updated_at
            from agent_definition
            where id = #{agentId}
            """)
    AgentRecord findById(@Param("agentId") long agentId);

    /**
     * 按业务标识查询 Agent。
     *
     * @param agentKey Agent 业务标识
     * @return Agent 记录
     */
    @ConstructorArgs({
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "agent_key", javaType = String.class),
            @Arg(column = "name", javaType = String.class),
            @Arg(column = "description", javaType = String.class),
            @Arg(column = "status", javaType = EnableStatus.class),
            @Arg(column = "system_prompt", javaType = String.class),
            @Arg(column = "default_model", javaType = String.class),
            @Arg(column = "temperature", javaType = BigDecimal.class),
            @Arg(column = "timeout_seconds", javaType = int.class),
            @Arg(column = "max_steps", javaType = int.class),
            @Arg(column = "current_draft_workflow_version_id", javaType = Long.class),
            @Arg(column = "current_published_workflow_version_id", javaType = Long.class),
            @Arg(column = "created_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "updated_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class)
    })
    @Select("""
            select id, agent_key, name, description, status, system_prompt, default_model, temperature,
                   timeout_seconds, max_steps, current_draft_workflow_version_id,
                   current_published_workflow_version_id, created_at, updated_at
            from agent_definition
            where agent_key = #{agentKey}
            """)
    AgentRecord findByAgentKey(@Param("agentKey") String agentKey);

    /**
     * 插入 Agent。
     *
     * @param record Agent 记录
     * @return 受影响行数
     */
    @Insert("""
            insert into agent_definition(
              agent_key, name, description, status, system_prompt, default_model, temperature,
              timeout_seconds, max_steps, current_draft_workflow_version_id, current_published_workflow_version_id
            )
            values (
              #{record.agentKey},
              #{record.name},
              #{record.description},
              #{record.status},
              #{record.systemPrompt},
              #{record.defaultModel},
              #{record.temperature},
              #{record.timeoutSeconds},
              #{record.maxSteps},
              #{record.currentDraftWorkflowVersionId},
              #{record.currentPublishedWorkflowVersionId}
            )
            """)
    int insert(@Param("record") AgentRecord record);

    /**
     * 更新 Agent 基础信息。
     *
     * @param record Agent 记录
     * @return 受影响行数
     */
    @Update("""
            update agent_definition
            set name = #{record.name},
                description = #{record.description},
                system_prompt = #{record.systemPrompt},
                default_model = #{record.defaultModel},
                temperature = #{record.temperature},
                timeout_seconds = #{record.timeoutSeconds},
                max_steps = #{record.maxSteps},
                updated_at = now()
            where id = #{record.id}
            """)
    int update(@Param("record") AgentRecord record);

    /**
     * 更新 Agent 状态。
     *
     * @param agentId Agent 主键
     * @param status 状态
     * @return 受影响行数
     */
    @Update("""
            update agent_definition
            set status = #{status},
                updated_at = now()
            where id = #{agentId}
            """)
    int updateStatus(@Param("agentId") long agentId, @Param("status") EnableStatus status);

    /**
     * 更新当前草稿版本指针。
     *
     * @param agentId Agent 主键
     * @param workflowVersionId 工作流版本主键
     * @return 受影响行数
     */
    @Update("""
            update agent_definition
            set current_draft_workflow_version_id = #{workflowVersionId},
                updated_at = now()
            where id = #{agentId}
            """)
    int updateCurrentDraftWorkflowVersionId(
            @Param("agentId") long agentId,
            @Param("workflowVersionId") Long workflowVersionId
    );

    /**
     * 更新当前发布版本指针。
     *
     * @param agentId Agent 主键
     * @param workflowVersionId 工作流版本主键
     * @return 受影响行数
     */
    @Update("""
            update agent_definition
            set current_published_workflow_version_id = #{workflowVersionId},
                updated_at = now()
            where id = #{agentId}
            """)
    int updateCurrentPublishedWorkflowVersionId(
            @Param("agentId") long agentId,
            @Param("workflowVersionId") Long workflowVersionId
    );
}
