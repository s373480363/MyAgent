package com.myagent.modelcatalog.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.myagent.common.domain.EnableStatus;
import com.myagent.common.repository.InstantTypeHandler;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;

/**
 * 模型供应项 MyBatis Mapper。
 */
@Mapper
public interface ModelOfferingMapper {

    /**
     * 分页查询模型供应项。
     *
     * @param providerKey 供应商标识
     * @param keyword 关键词
     * @param status 状态
     * @param limit 限制条数
     * @param offset 偏移量
     * @return 联表记录列表
     */
    @ConstructorArgs({
            @Arg(column = "offering_id", javaType = long.class),
            @Arg(column = "offering_key", javaType = String.class),
            @Arg(column = "provider_key", javaType = String.class),
            @Arg(column = "provider_name", javaType = String.class),
            @Arg(column = "model_key", javaType = String.class),
            @Arg(column = "display_name", javaType = String.class),
            @Arg(column = "upstream_model_name", javaType = String.class),
            @Arg(column = "default_temperature", javaType = BigDecimal.class),
            @Arg(column = "status", javaType = EnableStatus.class),
            @Arg(column = "provider_status", javaType = EnableStatus.class),
            @Arg(column = "description", javaType = String.class),
            @Arg(column = "created_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "updated_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class)
    })
    @Select({
            "<script>",
            "select",
            "  o.id as offering_id,",
            "  o.offering_key,",
            "  o.provider_key,",
            "  p.name as provider_name,",
            "  o.model_key,",
            "  o.display_name,",
            "  o.upstream_model_name,",
            "  o.default_temperature,",
            "  o.status,",
            "  p.status as provider_status,",
            "  o.description,",
            "  o.created_at,",
            "  o.updated_at",
            "from model_offering o",
            "join model_provider p on p.provider_key = o.provider_key",
            "<where>",
            "  <if test='providerKey != null and providerKey != \"\"'>",
            "    and o.provider_key = #{providerKey}",
            "  </if>",
            "  <if test='keyword != null and keyword != \"\"'>",
            "    and (",
            "      lower(o.offering_key) like concat('%', lower(#{keyword}), '%')",
            "      or lower(o.model_key) like concat('%', lower(#{keyword}), '%')",
            "      or lower(o.display_name) like concat('%', lower(#{keyword}), '%')",
            "      or lower(o.upstream_model_name) like concat('%', lower(#{keyword}), '%')",
            "      or lower(p.name) like concat('%', lower(#{keyword}), '%')",
            "    )",
            "  </if>",
            "  <choose>",
            "    <when test='status != null'>",
            "      and o.status = #{status}",
            "    </when>",
            "    <otherwise>",
            "      and o.status = 'ENABLED'",
            "      and p.status = 'ENABLED'",
            "    </otherwise>",
            "  </choose>",
            "</where>",
            "order by o.updated_at desc, o.id desc",
            "limit #{limit} offset #{offset}",
            "</script>"
    })
    List<ModelOfferingJoinedRecord> listOfferings(
            @Param("providerKey") String providerKey,
            @Param("keyword") String keyword,
            @Param("status") EnableStatus status,
            @Param("limit") long limit,
            @Param("offset") long offset
    );

    /**
     * 统计模型供应项数量。
     *
     * @param providerKey 供应商标识
     * @param keyword 关键词
     * @param status 状态
     * @return 数量
     */
    @Select({
            "<script>",
            "select count(*)",
            "from model_offering o",
            "join model_provider p on p.provider_key = o.provider_key",
            "<where>",
            "  <if test='providerKey != null and providerKey != \"\"'>",
            "    and o.provider_key = #{providerKey}",
            "  </if>",
            "  <if test='keyword != null and keyword != \"\"'>",
            "    and (",
            "      lower(o.offering_key) like concat('%', lower(#{keyword}), '%')",
            "      or lower(o.model_key) like concat('%', lower(#{keyword}), '%')",
            "      or lower(o.display_name) like concat('%', lower(#{keyword}), '%')",
            "      or lower(o.upstream_model_name) like concat('%', lower(#{keyword}), '%')",
            "      or lower(p.name) like concat('%', lower(#{keyword}), '%')",
            "    )",
            "  </if>",
            "  <choose>",
            "    <when test='status != null'>",
            "      and o.status = #{status}",
            "    </when>",
            "    <otherwise>",
            "      and o.status = 'ENABLED'",
            "      and p.status = 'ENABLED'",
            "    </otherwise>",
            "  </choose>",
            "</where>",
            "</script>"
    })
    long countOfferings(
            @Param("providerKey") String providerKey,
            @Param("keyword") String keyword,
            @Param("status") EnableStatus status
    );

    /**
     * 按主键查询模型供应项。
     *
     * @param offeringId 供应项主键
     * @return 供应项记录
     */
    @ConstructorArgs({
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "offering_key", javaType = String.class),
            @Arg(column = "provider_key", javaType = String.class),
            @Arg(column = "model_key", javaType = String.class),
            @Arg(column = "display_name", javaType = String.class),
            @Arg(column = "upstream_model_name", javaType = String.class),
            @Arg(column = "default_temperature", javaType = BigDecimal.class),
            @Arg(column = "status", javaType = EnableStatus.class),
            @Arg(column = "description", javaType = String.class),
            @Arg(column = "created_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "updated_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class)
    })
    @Select("""
            select id, offering_key, provider_key, model_key, display_name, upstream_model_name,
                   default_temperature, status, description, created_at, updated_at
            from model_offering
            where id = #{offeringId}
            """)
    ModelOfferingRecord findById(@Param("offeringId") long offeringId);

    /**
     * 按键查询模型供应项。
     *
     * @param offeringKey 供应项标识
     * @return 供应项记录
     */
    @ConstructorArgs({
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "offering_key", javaType = String.class),
            @Arg(column = "provider_key", javaType = String.class),
            @Arg(column = "model_key", javaType = String.class),
            @Arg(column = "display_name", javaType = String.class),
            @Arg(column = "upstream_model_name", javaType = String.class),
            @Arg(column = "default_temperature", javaType = BigDecimal.class),
            @Arg(column = "status", javaType = EnableStatus.class),
            @Arg(column = "description", javaType = String.class),
            @Arg(column = "created_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "updated_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class)
    })
    @Select("""
            select id, offering_key, provider_key, model_key, display_name, upstream_model_name,
                   default_temperature, status, description, created_at, updated_at
            from model_offering
            where offering_key = #{offeringKey}
            """)
    ModelOfferingRecord findByOfferingKey(@Param("offeringKey") String offeringKey);

    /**
     * 按供应商和上游模型名查询模型供应项。
     *
     * @param providerKey 供应商标识
     * @param upstreamModelName 上游模型名
     * @return 供应项记录
     */
    @ConstructorArgs({
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "offering_key", javaType = String.class),
            @Arg(column = "provider_key", javaType = String.class),
            @Arg(column = "model_key", javaType = String.class),
            @Arg(column = "display_name", javaType = String.class),
            @Arg(column = "upstream_model_name", javaType = String.class),
            @Arg(column = "default_temperature", javaType = BigDecimal.class),
            @Arg(column = "status", javaType = EnableStatus.class),
            @Arg(column = "description", javaType = String.class),
            @Arg(column = "created_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "updated_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class)
    })
    @Select("""
            select id, offering_key, provider_key, model_key, display_name, upstream_model_name,
                   default_temperature, status, description, created_at, updated_at
            from model_offering
            where provider_key = #{providerKey}
              and upstream_model_name = #{upstreamModelName}
            """)
    ModelOfferingRecord findByProviderKeyAndUpstreamModelName(
            @Param("providerKey") String providerKey,
            @Param("upstreamModelName") String upstreamModelName
    );

    /**
     * 按键查询联表视图。
     *
     * @param offeringKey 供应项标识
     * @return 联表记录
     */
    @ConstructorArgs({
            @Arg(column = "offering_id", javaType = long.class),
            @Arg(column = "offering_key", javaType = String.class),
            @Arg(column = "provider_key", javaType = String.class),
            @Arg(column = "provider_name", javaType = String.class),
            @Arg(column = "model_key", javaType = String.class),
            @Arg(column = "display_name", javaType = String.class),
            @Arg(column = "upstream_model_name", javaType = String.class),
            @Arg(column = "default_temperature", javaType = BigDecimal.class),
            @Arg(column = "status", javaType = EnableStatus.class),
            @Arg(column = "provider_status", javaType = EnableStatus.class),
            @Arg(column = "description", javaType = String.class),
            @Arg(column = "created_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "updated_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class)
    })
    @Select("""
            select
              o.id as offering_id,
              o.offering_key,
              o.provider_key,
              p.name as provider_name,
              o.model_key,
              o.display_name,
              o.upstream_model_name,
              o.default_temperature,
              o.status,
              p.status as provider_status,
              o.description,
              o.created_at,
              o.updated_at
            from model_offering o
            join model_provider p on p.provider_key = o.provider_key
            where o.offering_key = #{offeringKey}
            """)
    ModelOfferingJoinedRecord findJoinedByOfferingKey(@Param("offeringKey") String offeringKey);

    /**
     * 按键批量查询联表视图。
     *
     * @param offeringKeys 供应项键列表
     * @return 联表记录列表
     */
    @ConstructorArgs({
            @Arg(column = "offering_id", javaType = long.class),
            @Arg(column = "offering_key", javaType = String.class),
            @Arg(column = "provider_key", javaType = String.class),
            @Arg(column = "provider_name", javaType = String.class),
            @Arg(column = "model_key", javaType = String.class),
            @Arg(column = "display_name", javaType = String.class),
            @Arg(column = "upstream_model_name", javaType = String.class),
            @Arg(column = "default_temperature", javaType = BigDecimal.class),
            @Arg(column = "status", javaType = EnableStatus.class),
            @Arg(column = "provider_status", javaType = EnableStatus.class),
            @Arg(column = "description", javaType = String.class),
            @Arg(column = "created_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "updated_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class)
    })
    @Select({
            "<script>",
            "select",
            "  o.id as offering_id,",
            "  o.offering_key,",
            "  o.provider_key,",
            "  p.name as provider_name,",
            "  o.model_key,",
            "  o.display_name,",
            "  o.upstream_model_name,",
            "  o.default_temperature,",
            "  o.status,",
            "  p.status as provider_status,",
            "  o.description,",
            "  o.created_at,",
            "  o.updated_at",
            "from model_offering o",
            "join model_provider p on p.provider_key = o.provider_key",
            "where o.offering_key in",
            "<foreach collection='offeringKeys' item='offeringKey' open='(' separator=',' close=')'>",
            "  #{offeringKey}",
            "</foreach>",
            "</script>"
    })
    List<ModelOfferingJoinedRecord> findJoinedByOfferingKeys(@Param("offeringKeys") List<String> offeringKeys);

    /**
     * 插入模型供应项。
     *
     * @param record 供应项记录
     * @return 新增后的记录
     */
    @ConstructorArgs({
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "offering_key", javaType = String.class),
            @Arg(column = "provider_key", javaType = String.class),
            @Arg(column = "model_key", javaType = String.class),
            @Arg(column = "display_name", javaType = String.class),
            @Arg(column = "upstream_model_name", javaType = String.class),
            @Arg(column = "default_temperature", javaType = BigDecimal.class),
            @Arg(column = "status", javaType = EnableStatus.class),
            @Arg(column = "description", javaType = String.class),
            @Arg(column = "created_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "updated_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class)
    })
    @Select("""
            insert into model_offering(
              offering_key, provider_key, model_key, display_name, upstream_model_name, default_temperature, status, description
            )
            values (
              #{record.offeringKey},
              #{record.providerKey},
              #{record.modelKey},
              #{record.displayName},
              #{record.upstreamModelName},
              #{record.defaultTemperature},
              #{record.status},
              #{record.description}
            )
            returning id, offering_key, provider_key, model_key, display_name, upstream_model_name,
                      default_temperature, status, description, created_at, updated_at
            """)
    ModelOfferingRecord insert(@Param("record") ModelOfferingRecord record);

    /**
     * 更新模型供应项。
     *
     * @param record 供应项记录
     * @return 受影响行数
     */
    @Update("""
            update model_offering
            set provider_key = #{record.providerKey},
                model_key = #{record.modelKey},
                display_name = #{record.displayName},
                upstream_model_name = #{record.upstreamModelName},
                default_temperature = #{record.defaultTemperature},
                status = #{record.status},
                description = #{record.description},
                updated_at = now()
            where id = #{record.id}
            """)
    int update(@Param("record") ModelOfferingRecord record);

    /**
     * 更新模型供应项状态。
     *
     * @param offeringId 供应项主键
     * @param status 状态
     * @return 受影响行数
     */
    @Update("""
            update model_offering
            set status = #{status},
                updated_at = now()
            where id = #{offeringId}
            """)
    int updateStatus(@Param("offeringId") long offeringId, @Param("status") EnableStatus status);
}
