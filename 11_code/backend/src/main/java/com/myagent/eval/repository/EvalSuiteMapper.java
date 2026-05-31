package com.myagent.eval.repository;

import com.myagent.common.repository.InstantTypeHandler;
import com.myagent.eval.domain.EvalSuiteStatus;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

/**
 * EvalSuite MyBatis Mapper。
 */
@Mapper
public interface EvalSuiteMapper {

    /**
     * 新增验收套件。
     *
     * @param record 套件记录
     * @return 新增后的套件
     */
    @ConstructorArgs({
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "agent_id", javaType = long.class),
            @Arg(column = "workflow_version_id", javaType = long.class),
            @Arg(column = "node_id", javaType = String.class),
            @Arg(column = "name", javaType = String.class),
            @Arg(column = "goal", javaType = String.class),
            @Arg(column = "pass_threshold", javaType = BigDecimal.class),
            @Arg(column = "status", javaType = EvalSuiteStatus.class),
            @Arg(column = "created_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "updated_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class)
    })
    @Select("""
            insert into eval_suite(agent_id, workflow_version_id, node_id, name, goal, pass_threshold, status)
            values (
              #{record.agentId},
              #{record.workflowVersionId},
              #{record.nodeId},
              #{record.name},
              #{record.goal},
              #{record.passThreshold},
              #{record.status}
            )
            returning id, agent_id, workflow_version_id, node_id, name, goal, pass_threshold, status, created_at, updated_at
            """)
    EvalSuiteRecord insert(@Param("record") EvalSuiteRecord record);

    /**
     * 按主键查询。
     *
     * @param suiteId 套件主键
     * @return 套件记录
     */
    @ConstructorArgs({
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "agent_id", javaType = long.class),
            @Arg(column = "workflow_version_id", javaType = long.class),
            @Arg(column = "node_id", javaType = String.class),
            @Arg(column = "name", javaType = String.class),
            @Arg(column = "goal", javaType = String.class),
            @Arg(column = "pass_threshold", javaType = BigDecimal.class),
            @Arg(column = "status", javaType = EvalSuiteStatus.class),
            @Arg(column = "created_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "updated_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class)
    })
    @Select("""
            select id, agent_id, workflow_version_id, node_id, name, goal, pass_threshold, status, created_at, updated_at
            from eval_suite
            where id = #{suiteId}
            """)
    EvalSuiteRecord findById(@Param("suiteId") long suiteId);

    /**
     * 分页查询验收套件。
     *
     * @param agentId Agent 主键
     * @param workflowVersionId 工作流版本主键
     * @param nodeId 节点标识
     * @param status 套件状态
     * @param keyword 关键词
     * @param limit 限制条数
     * @param offset 偏移量
     * @return 套件记录列表
     */
    @ConstructorArgs({
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "agent_id", javaType = long.class),
            @Arg(column = "workflow_version_id", javaType = long.class),
            @Arg(column = "node_id", javaType = String.class),
            @Arg(column = "name", javaType = String.class),
            @Arg(column = "goal", javaType = String.class),
            @Arg(column = "pass_threshold", javaType = BigDecimal.class),
            @Arg(column = "status", javaType = EvalSuiteStatus.class),
            @Arg(column = "created_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "updated_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class)
    })
    @Select({
            "<script>",
            "select id, agent_id, workflow_version_id, node_id, name, goal, pass_threshold, status, created_at, updated_at",
            "from eval_suite",
            "<where>",
            "  <if test='agentId != null'> and agent_id = #{agentId} </if>",
            "  <if test='workflowVersionId != null'> and workflow_version_id = #{workflowVersionId} </if>",
            "  <if test='nodeId != null and nodeId != \"\"'> and node_id = #{nodeId} </if>",
            "  <if test='status != null'> and status = #{status} </if>",
            "  <if test='keyword != null and keyword != \"\"'>",
            "    and (lower(name) like concat('%', lower(#{keyword}), '%')",
            "         or lower(goal) like concat('%', lower(#{keyword}), '%')",
            "         or lower(node_id) like concat('%', lower(#{keyword}), '%'))",
            "  </if>",
            "</where>",
            "order by updated_at desc, id desc",
            "limit #{limit} offset #{offset}",
            "</script>"
    })
    List<EvalSuiteRecord> list(
            @Param("agentId") Long agentId,
            @Param("workflowVersionId") Long workflowVersionId,
            @Param("nodeId") String nodeId,
            @Param("status") EvalSuiteStatus status,
            @Param("keyword") String keyword,
            @Param("limit") long limit,
            @Param("offset") long offset
    );

    /**
     * 统计验收套件数量。
     *
     * @param agentId Agent 主键
     * @param workflowVersionId 工作流版本主键
     * @param nodeId 节点标识
     * @param status 套件状态
     * @param keyword 关键词
     * @return 总数
     */
    @Select({
            "<script>",
            "select count(*) from eval_suite",
            "<where>",
            "  <if test='agentId != null'> and agent_id = #{agentId} </if>",
            "  <if test='workflowVersionId != null'> and workflow_version_id = #{workflowVersionId} </if>",
            "  <if test='nodeId != null and nodeId != \"\"'> and node_id = #{nodeId} </if>",
            "  <if test='status != null'> and status = #{status} </if>",
            "  <if test='keyword != null and keyword != \"\"'>",
            "    and (lower(name) like concat('%', lower(#{keyword}), '%')",
            "         or lower(goal) like concat('%', lower(#{keyword}), '%')",
            "         or lower(node_id) like concat('%', lower(#{keyword}), '%'))",
            "  </if>",
            "</where>",
            "</script>"
    })
    long count(
            @Param("agentId") Long agentId,
            @Param("workflowVersionId") Long workflowVersionId,
            @Param("nodeId") String nodeId,
            @Param("status") EvalSuiteStatus status,
            @Param("keyword") String keyword
    );

    /**
     * 更新验收套件。
     *
     * @param suiteId 套件主键
     * @param name 名称
     * @param goal 目标
     * @param passThreshold 阈值
     * @return 更新后的套件
     */
    @ConstructorArgs({
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "agent_id", javaType = long.class),
            @Arg(column = "workflow_version_id", javaType = long.class),
            @Arg(column = "node_id", javaType = String.class),
            @Arg(column = "name", javaType = String.class),
            @Arg(column = "goal", javaType = String.class),
            @Arg(column = "pass_threshold", javaType = BigDecimal.class),
            @Arg(column = "status", javaType = EvalSuiteStatus.class),
            @Arg(column = "created_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "updated_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class)
    })
    @Select("""
            update eval_suite
            set name = #{name},
                goal = #{goal},
                pass_threshold = #{passThreshold},
                updated_at = now()
            where id = #{suiteId}
            returning id, agent_id, workflow_version_id, node_id, name, goal, pass_threshold, status, created_at, updated_at
            """)
    EvalSuiteRecord update(
            @Param("suiteId") long suiteId,
            @Param("name") String name,
            @Param("goal") String goal,
            @Param("passThreshold") BigDecimal passThreshold
    );

    /**
     * 更新验收套件状态。
     *
     * @param suiteId 套件主键
     * @param status 目标状态
     * @return 更新后的套件
     */
    @ConstructorArgs({
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "agent_id", javaType = long.class),
            @Arg(column = "workflow_version_id", javaType = long.class),
            @Arg(column = "node_id", javaType = String.class),
            @Arg(column = "name", javaType = String.class),
            @Arg(column = "goal", javaType = String.class),
            @Arg(column = "pass_threshold", javaType = BigDecimal.class),
            @Arg(column = "status", javaType = EvalSuiteStatus.class),
            @Arg(column = "created_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "updated_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class)
    })
    @Select("""
            update eval_suite
            set status = #{status},
                updated_at = now()
            where id = #{suiteId}
            returning id, agent_id, workflow_version_id, node_id, name, goal, pass_threshold, status, created_at, updated_at
            """)
    EvalSuiteRecord updateStatus(@Param("suiteId") long suiteId, @Param("status") EvalSuiteStatus status);
}
