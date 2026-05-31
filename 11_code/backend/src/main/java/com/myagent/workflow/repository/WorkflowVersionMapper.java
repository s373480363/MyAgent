package com.myagent.workflow.repository;

import com.myagent.common.repository.InstantTypeHandler;
import com.myagent.workflow.domain.WorkflowVersionStatus;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 工作流版本 MyBatis Mapper。
 */
@Mapper
public interface WorkflowVersionMapper {

    /**
     * 按主键查询工作流版本。
     *
     * @param workflowVersionId 工作流版本主键
     * @return 版本记录
     */
    @ConstructorArgs({
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "agent_id", javaType = long.class),
            @Arg(column = "version_no", javaType = int.class),
            @Arg(column = "status", javaType = WorkflowVersionStatus.class),
            @Arg(column = "nodes_json", javaType = java.util.List.class, typeHandler = WorkflowNodeDefinitionListTypeHandler.class),
            @Arg(column = "edges_json", javaType = java.util.List.class, typeHandler = WorkflowEdgeDefinitionListTypeHandler.class),
            @Arg(column = "runtime_options_json", javaType = com.myagent.workflow.domain.WorkflowRuntimeOptions.class, typeHandler = WorkflowRuntimeOptionsTypeHandler.class),
            @Arg(column = "referenced_schema_versions_json", javaType = java.util.List.class, typeHandler = ReferencedSchemaVersionListTypeHandler.class),
            @Arg(column = "source_workflow_version_id", javaType = Long.class),
            @Arg(column = "published_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "created_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "updated_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class)
    })
    @Select("""
            select id, agent_id, version_no, status, nodes_json, edges_json, runtime_options_json,
                   referenced_schema_versions_json, source_workflow_version_id, published_at, created_at, updated_at
            from workflow_version
            where id = #{workflowVersionId}
            """)
    WorkflowVersionRecord findById(@Param("workflowVersionId") long workflowVersionId);

    /**
     * 查询当前草稿版本。
     *
     * @param agentId Agent 主键
     * @return 草稿版本记录
     */
    @ConstructorArgs({
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "agent_id", javaType = long.class),
            @Arg(column = "version_no", javaType = int.class),
            @Arg(column = "status", javaType = WorkflowVersionStatus.class),
            @Arg(column = "nodes_json", javaType = java.util.List.class, typeHandler = WorkflowNodeDefinitionListTypeHandler.class),
            @Arg(column = "edges_json", javaType = java.util.List.class, typeHandler = WorkflowEdgeDefinitionListTypeHandler.class),
            @Arg(column = "runtime_options_json", javaType = com.myagent.workflow.domain.WorkflowRuntimeOptions.class, typeHandler = WorkflowRuntimeOptionsTypeHandler.class),
            @Arg(column = "referenced_schema_versions_json", javaType = java.util.List.class, typeHandler = ReferencedSchemaVersionListTypeHandler.class),
            @Arg(column = "source_workflow_version_id", javaType = Long.class),
            @Arg(column = "published_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "created_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "updated_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class)
    })
    @Select("""
            select id, agent_id, version_no, status, nodes_json, edges_json, runtime_options_json,
                   referenced_schema_versions_json, source_workflow_version_id, published_at, created_at, updated_at
            from workflow_version
            where agent_id = #{agentId} and status = 'DRAFT'
            """)
    WorkflowVersionRecord findCurrentDraft(@Param("agentId") long agentId);

    /**
     * 查询当前发布版本。
     *
     * @param agentId Agent 主键
     * @return 发布版本记录
     */
    @ConstructorArgs({
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "agent_id", javaType = long.class),
            @Arg(column = "version_no", javaType = int.class),
            @Arg(column = "status", javaType = WorkflowVersionStatus.class),
            @Arg(column = "nodes_json", javaType = java.util.List.class, typeHandler = WorkflowNodeDefinitionListTypeHandler.class),
            @Arg(column = "edges_json", javaType = java.util.List.class, typeHandler = WorkflowEdgeDefinitionListTypeHandler.class),
            @Arg(column = "runtime_options_json", javaType = com.myagent.workflow.domain.WorkflowRuntimeOptions.class, typeHandler = WorkflowRuntimeOptionsTypeHandler.class),
            @Arg(column = "referenced_schema_versions_json", javaType = java.util.List.class, typeHandler = ReferencedSchemaVersionListTypeHandler.class),
            @Arg(column = "source_workflow_version_id", javaType = Long.class),
            @Arg(column = "published_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "created_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "updated_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class)
    })
    @Select("""
            select id, agent_id, version_no, status, nodes_json, edges_json, runtime_options_json,
                   referenced_schema_versions_json, source_workflow_version_id, published_at, created_at, updated_at
            from workflow_version
            where agent_id = #{agentId} and status = 'PUBLISHED'
            """)
    WorkflowVersionRecord findCurrentPublished(@Param("agentId") long agentId);

    /**
     * 查询最大版本号。
     *
     * @param agentId Agent 主键
     * @return 最大版本号
     */
    @Select("""
            select max(version_no)
            from workflow_version
            where agent_id = #{agentId}
            """)
    Integer findMaxVersionNo(@Param("agentId") long agentId);

    /**
     * 按 Agent 和版本号查询工作流版本。
     *
     * @param agentId Agent 主键
     * @param versionNo 版本号
     * @return 版本记录
     */
    @ConstructorArgs({
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "agent_id", javaType = long.class),
            @Arg(column = "version_no", javaType = int.class),
            @Arg(column = "status", javaType = WorkflowVersionStatus.class),
            @Arg(column = "nodes_json", javaType = java.util.List.class, typeHandler = WorkflowNodeDefinitionListTypeHandler.class),
            @Arg(column = "edges_json", javaType = java.util.List.class, typeHandler = WorkflowEdgeDefinitionListTypeHandler.class),
            @Arg(column = "runtime_options_json", javaType = com.myagent.workflow.domain.WorkflowRuntimeOptions.class, typeHandler = WorkflowRuntimeOptionsTypeHandler.class),
            @Arg(column = "referenced_schema_versions_json", javaType = java.util.List.class, typeHandler = ReferencedSchemaVersionListTypeHandler.class),
            @Arg(column = "source_workflow_version_id", javaType = Long.class),
            @Arg(column = "published_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "created_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "updated_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class)
    })
    @Select("""
            select id, agent_id, version_no, status, nodes_json, edges_json, runtime_options_json,
                   referenced_schema_versions_json, source_workflow_version_id, published_at, created_at, updated_at
            from workflow_version
            where agent_id = #{agentId} and version_no = #{versionNo}
            """)
    WorkflowVersionRecord findByAgentIdAndVersionNo(
            @Param("agentId") long agentId,
            @Param("versionNo") int versionNo
    );

    /**
     * 插入工作流版本。
     *
     * @param record 工作流版本记录
     * @return 受影响行数
     */
    @Insert("""
            insert into workflow_version(
              agent_id, version_no, status, nodes_json, edges_json, runtime_options_json,
              referenced_schema_versions_json, source_workflow_version_id, published_at
            )
            values (
              #{record.agentId},
              #{record.versionNo},
              #{record.status},
              #{record.nodes, typeHandler=com.myagent.workflow.repository.WorkflowNodeDefinitionListTypeHandler},
              #{record.edges, typeHandler=com.myagent.workflow.repository.WorkflowEdgeDefinitionListTypeHandler},
              #{record.runtimeOptions, typeHandler=com.myagent.workflow.repository.WorkflowRuntimeOptionsTypeHandler},
              #{record.referencedSchemaVersions, typeHandler=com.myagent.workflow.repository.ReferencedSchemaVersionListTypeHandler},
              #{record.sourceWorkflowVersionId},
              #{record.publishedAt}
            )
            """)
    int insert(@Param("record") WorkflowVersionRecord record);

    /**
     * 更新工作流版本状态。
     *
     * @param workflowVersionId 工作流版本主键
     * @param status 新状态
     * @return 受影响行数
     */
    @Update("""
            update workflow_version
            set status = #{status},
                updated_at = now()
            where id = #{workflowVersionId}
            """)
    int updateStatus(
            @Param("workflowVersionId") long workflowVersionId,
            @Param("status") WorkflowVersionStatus status
    );

    /**
     * 分页查询工作流版本列表。
     *
     * @param agentId Agent 主键
     * @param status 状态过滤
     * @param limit 限制条数
     * @param offset 偏移量
     * @return 工作流版本列表
     */
    @ConstructorArgs({
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "agent_id", javaType = long.class),
            @Arg(column = "version_no", javaType = int.class),
            @Arg(column = "status", javaType = WorkflowVersionStatus.class),
            @Arg(column = "nodes_json", javaType = java.util.List.class, typeHandler = WorkflowNodeDefinitionListTypeHandler.class),
            @Arg(column = "edges_json", javaType = java.util.List.class, typeHandler = WorkflowEdgeDefinitionListTypeHandler.class),
            @Arg(column = "runtime_options_json", javaType = com.myagent.workflow.domain.WorkflowRuntimeOptions.class, typeHandler = WorkflowRuntimeOptionsTypeHandler.class),
            @Arg(column = "referenced_schema_versions_json", javaType = java.util.List.class, typeHandler = ReferencedSchemaVersionListTypeHandler.class),
            @Arg(column = "source_workflow_version_id", javaType = Long.class),
            @Arg(column = "published_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "created_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "updated_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class)
    })
    @Select({
            "<script>",
            "select id, agent_id, version_no, status, nodes_json, edges_json, runtime_options_json,",
            "       referenced_schema_versions_json, source_workflow_version_id, published_at, created_at, updated_at",
            "from workflow_version",
            "where agent_id = #{agentId}",
            "  <if test='status != null'>",
            "    and status = #{status}",
            "  </if>",
            "order by version_no desc, id desc",
            "limit #{limit} offset #{offset}",
            "</script>"
    })
    List<WorkflowVersionRecord> listWorkflowVersions(
            @Param("agentId") long agentId,
            @Param("status") WorkflowVersionStatus status,
            @Param("limit") long limit,
            @Param("offset") long offset
    );

    /**
     * 统计工作流版本数量。
     *
     * @param agentId Agent 主键
     * @param status 状态过滤
     * @return 总数
     */
    @Select({
            "<script>",
            "select count(*)",
            "from workflow_version",
            "where agent_id = #{agentId}",
            "  <if test='status != null'>",
            "    and status = #{status}",
            "  </if>",
            "</script>"
    })
    long countWorkflowVersions(
            @Param("agentId") long agentId,
            @Param("status") WorkflowVersionStatus status
    );

    /**
     * 统计历史版本数量。
     *
     * @param agentId Agent 主键
     * @return 历史版本数量
     */
    @Select("""
            select count(*)
            from workflow_version
            where agent_id = #{agentId} and status = 'HISTORY'
            """)
    long countHistory(@Param("agentId") long agentId);

    /**
     * 查询最近历史版本。
     *
     * @param agentId Agent 主键
     * @return 最近历史版本
     */
    @ConstructorArgs({
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "agent_id", javaType = long.class),
            @Arg(column = "version_no", javaType = int.class),
            @Arg(column = "status", javaType = WorkflowVersionStatus.class),
            @Arg(column = "nodes_json", javaType = java.util.List.class, typeHandler = WorkflowNodeDefinitionListTypeHandler.class),
            @Arg(column = "edges_json", javaType = java.util.List.class, typeHandler = WorkflowEdgeDefinitionListTypeHandler.class),
            @Arg(column = "runtime_options_json", javaType = com.myagent.workflow.domain.WorkflowRuntimeOptions.class, typeHandler = WorkflowRuntimeOptionsTypeHandler.class),
            @Arg(column = "referenced_schema_versions_json", javaType = java.util.List.class, typeHandler = ReferencedSchemaVersionListTypeHandler.class),
            @Arg(column = "source_workflow_version_id", javaType = Long.class),
            @Arg(column = "published_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "created_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "updated_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class)
    })
    @Select("""
            select id, agent_id, version_no, status, nodes_json, edges_json, runtime_options_json,
                   referenced_schema_versions_json, source_workflow_version_id, published_at, created_at, updated_at
            from workflow_version
            where agent_id = #{agentId} and status = 'HISTORY'
            order by version_no desc, id desc
            limit 1
            """)
    WorkflowVersionRecord findLatestHistory(@Param("agentId") long agentId);
}
