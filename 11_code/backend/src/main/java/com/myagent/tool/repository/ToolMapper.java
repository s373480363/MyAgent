package com.myagent.tool.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.myagent.common.domain.EnableStatus;
import com.myagent.common.repository.InstantTypeHandler;
import com.myagent.common.repository.JsonNodeTypeHandler;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 工具 MyBatis Mapper。
 */
@Mapper
public interface ToolMapper {

    /**
     * 分页查询工具。
     *
     * @param keyword 关键词
     * @param status 状态
     * @param executorType 执行器类型
     * @param limit 限制条数
     * @param offset 偏移量
     * @return 记录列表
     */
    @ConstructorArgs({
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "tool_key", javaType = String.class),
            @Arg(column = "name", javaType = String.class),
            @Arg(column = "description", javaType = String.class),
            @Arg(column = "input_schema_id", javaType = long.class),
            @Arg(column = "output_schema_id", javaType = long.class),
            @Arg(column = "executor_type", javaType = String.class),
            @Arg(column = "executor_config_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "status", javaType = EnableStatus.class),
            @Arg(column = "created_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "updated_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class)
    })
    @Select({
            "<script>",
            "select id, tool_key, name, description, input_schema_id, output_schema_id,",
            "       executor_type, executor_config_json, status, created_at, updated_at",
            "from tool_definition",
            "<where>",
            "  <if test='keyword != null and keyword != \"\"'>",
            "    and (",
            "      lower(tool_key) like concat('%', lower(#{keyword}), '%')",
            "      or lower(name) like concat('%', lower(#{keyword}), '%')",
            "      or lower(description) like concat('%', lower(#{keyword}), '%')",
            "    )",
            "  </if>",
            "  <if test='status != null'>",
            "    and status = #{status}",
            "  </if>",
            "  <if test='executorType != null and executorType != \"\"'>",
            "    and executor_type = #{executorType}",
            "  </if>",
            "</where>",
            "order by updated_at desc, id desc",
            "limit #{limit} offset #{offset}",
            "</script>"
    })
    List<ToolRecord> listTools(
            @Param("keyword") String keyword,
            @Param("status") EnableStatus status,
            @Param("executorType") String executorType,
            @Param("limit") long limit,
            @Param("offset") long offset
    );

    /**
     * 统计工具数量。
     *
     * @param keyword 关键词
     * @param status 状态
     * @param executorType 执行器类型
     * @return 总数
     */
    @Select({
            "<script>",
            "select count(*)",
            "from tool_definition",
            "<where>",
            "  <if test='keyword != null and keyword != \"\"'>",
            "    and (",
            "      lower(tool_key) like concat('%', lower(#{keyword}), '%')",
            "      or lower(name) like concat('%', lower(#{keyword}), '%')",
            "      or lower(description) like concat('%', lower(#{keyword}), '%')",
            "    )",
            "  </if>",
            "  <if test='status != null'>",
            "    and status = #{status}",
            "  </if>",
            "  <if test='executorType != null and executorType != \"\"'>",
            "    and executor_type = #{executorType}",
            "  </if>",
            "</where>",
            "</script>"
    })
    long countTools(
            @Param("keyword") String keyword,
            @Param("status") EnableStatus status,
            @Param("executorType") String executorType
    );

    /**
     * 按主键查询工具。
     *
     * @param toolId 工具主键
     * @return 工具记录
     */
    @ConstructorArgs({
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "tool_key", javaType = String.class),
            @Arg(column = "name", javaType = String.class),
            @Arg(column = "description", javaType = String.class),
            @Arg(column = "input_schema_id", javaType = long.class),
            @Arg(column = "output_schema_id", javaType = long.class),
            @Arg(column = "executor_type", javaType = String.class),
            @Arg(column = "executor_config_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "status", javaType = EnableStatus.class),
            @Arg(column = "created_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "updated_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class)
    })
    @Select("""
            select id, tool_key, name, description, input_schema_id, output_schema_id,
                   executor_type, executor_config_json, status, created_at, updated_at
            from tool_definition
            where id = #{toolId}
            """)
    ToolRecord findById(@Param("toolId") long toolId);

    /**
     * 按工具标识查询工具。
     *
     * @param toolKey 工具标识
     * @return 工具记录
     */
    @ConstructorArgs({
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "tool_key", javaType = String.class),
            @Arg(column = "name", javaType = String.class),
            @Arg(column = "description", javaType = String.class),
            @Arg(column = "input_schema_id", javaType = long.class),
            @Arg(column = "output_schema_id", javaType = long.class),
            @Arg(column = "executor_type", javaType = String.class),
            @Arg(column = "executor_config_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "status", javaType = EnableStatus.class),
            @Arg(column = "created_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "updated_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class)
    })
    @Select("""
            select id, tool_key, name, description, input_schema_id, output_schema_id,
                   executor_type, executor_config_json, status, created_at, updated_at
            from tool_definition
            where tool_key = #{toolKey}
            """)
    ToolRecord findByToolKey(@Param("toolKey") String toolKey);
}
