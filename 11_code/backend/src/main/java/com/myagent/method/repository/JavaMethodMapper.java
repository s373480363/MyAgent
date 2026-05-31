package com.myagent.method.repository;

import com.myagent.common.domain.EnableStatus;
import com.myagent.common.repository.InstantTypeHandler;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Java 方法 MyBatis Mapper。
 */
@Mapper
public interface JavaMethodMapper {

    /**
     * 分页查询 Java 方法。
     *
     * @param keyword 关键词
     * @param status 状态
     * @param limit 限制条数
     * @param offset 偏移量
     * @return 记录列表
     */
    @ConstructorArgs({
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "method_key", javaType = String.class),
            @Arg(column = "name", javaType = String.class),
            @Arg(column = "description", javaType = String.class),
            @Arg(column = "bean_name", javaType = String.class),
            @Arg(column = "method_name", javaType = String.class),
            @Arg(column = "input_schema_id", javaType = long.class),
            @Arg(column = "output_schema_id", javaType = long.class),
            @Arg(column = "status", javaType = EnableStatus.class),
            @Arg(column = "created_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "updated_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class)
    })
    @Select({
            "<script>",
            "select id, method_key, name, description, bean_name, method_name,",
            "       input_schema_id, output_schema_id, status, created_at, updated_at",
            "from java_method_definition",
            "<where>",
            "  <if test='keyword != null and keyword != \"\"'>",
            "    and (",
            "      lower(method_key) like concat('%', lower(#{keyword}), '%')",
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
    List<JavaMethodRecord> listJavaMethods(
            @Param("keyword") String keyword,
            @Param("status") EnableStatus status,
            @Param("limit") long limit,
            @Param("offset") long offset
    );

    /**
     * 统计 Java 方法数量。
     *
     * @param keyword 关键词
     * @param status 状态
     * @return 总数
     */
    @Select({
            "<script>",
            "select count(*)",
            "from java_method_definition",
            "<where>",
            "  <if test='keyword != null and keyword != \"\"'>",
            "    and (",
            "      lower(method_key) like concat('%', lower(#{keyword}), '%')",
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
    long countJavaMethods(@Param("keyword") String keyword, @Param("status") EnableStatus status);

    /**
     * 按主键查询 Java 方法。
     *
     * @param methodId 主键
     * @return Java 方法记录
     */
    @ConstructorArgs({
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "method_key", javaType = String.class),
            @Arg(column = "name", javaType = String.class),
            @Arg(column = "description", javaType = String.class),
            @Arg(column = "bean_name", javaType = String.class),
            @Arg(column = "method_name", javaType = String.class),
            @Arg(column = "input_schema_id", javaType = long.class),
            @Arg(column = "output_schema_id", javaType = long.class),
            @Arg(column = "status", javaType = EnableStatus.class),
            @Arg(column = "created_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "updated_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class)
    })
    @Select("""
            select id, method_key, name, description, bean_name, method_name,
                   input_schema_id, output_schema_id, status, created_at, updated_at
            from java_method_definition
            where id = #{methodId}
            """)
    JavaMethodRecord findById(@Param("methodId") long methodId);

    /**
     * 按方法标识查询 Java 方法。
     *
     * @param methodKey 方法标识
     * @return Java 方法记录
     */
    @ConstructorArgs({
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "method_key", javaType = String.class),
            @Arg(column = "name", javaType = String.class),
            @Arg(column = "description", javaType = String.class),
            @Arg(column = "bean_name", javaType = String.class),
            @Arg(column = "method_name", javaType = String.class),
            @Arg(column = "input_schema_id", javaType = long.class),
            @Arg(column = "output_schema_id", javaType = long.class),
            @Arg(column = "status", javaType = EnableStatus.class),
            @Arg(column = "created_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "updated_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class)
    })
    @Select("""
            select id, method_key, name, description, bean_name, method_name,
                   input_schema_id, output_schema_id, status, created_at, updated_at
            from java_method_definition
            where method_key = #{methodKey}
            """)
    JavaMethodRecord findByMethodKey(@Param("methodKey") String methodKey);
}
