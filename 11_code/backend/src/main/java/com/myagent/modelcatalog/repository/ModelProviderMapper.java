package com.myagent.modelcatalog.repository;

import com.myagent.common.domain.EnableStatus;
import com.myagent.common.repository.InstantTypeHandler;
import com.myagent.modelcatalog.domain.ModelProviderType;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 模型供应商 MyBatis Mapper。
 */
@Mapper
public interface ModelProviderMapper {

    /**
     * 分页查询模型供应商。
     *
     * @param keyword 关键词
     * @param status 状态
     * @param limit 限制条数
     * @param offset 偏移量
     * @return 记录列表
     */
    @ConstructorArgs({
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "provider_key", javaType = String.class),
            @Arg(column = "name", javaType = String.class),
            @Arg(column = "provider_type", javaType = ModelProviderType.class),
            @Arg(column = "base_url", javaType = String.class),
            @Arg(column = "api_key_ciphertext", javaType = String.class),
            @Arg(column = "api_key_mask", javaType = String.class),
            @Arg(column = "status", javaType = EnableStatus.class),
            @Arg(column = "description", javaType = String.class),
            @Arg(column = "created_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "updated_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class)
    })
    @Select({
            "<script>",
            "select id, provider_key, name, provider_type, base_url, api_key_ciphertext, api_key_mask, status, description, created_at, updated_at",
            "from model_provider",
            "<where>",
            "  <if test='keyword != null and keyword != \"\"'>",
            "    and (",
            "      lower(provider_key) like concat('%', lower(#{keyword}), '%')",
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
    List<ModelProviderRecord> listProviders(
            @Param("keyword") String keyword,
            @Param("status") EnableStatus status,
            @Param("limit") long limit,
            @Param("offset") long offset
    );

    /**
     * 统计模型供应商数量。
     *
     * @param keyword 关键词
     * @param status 状态
     * @return 数量
     */
    @Select({
            "<script>",
            "select count(*)",
            "from model_provider",
            "<where>",
            "  <if test='keyword != null and keyword != \"\"'>",
            "    and (",
            "      lower(provider_key) like concat('%', lower(#{keyword}), '%')",
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
    long countProviders(@Param("keyword") String keyword, @Param("status") EnableStatus status);

    /**
     * 按主键查询模型供应商。
     *
     * @param providerId 主键
     * @return 供应商记录
     */
    @ConstructorArgs({
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "provider_key", javaType = String.class),
            @Arg(column = "name", javaType = String.class),
            @Arg(column = "provider_type", javaType = ModelProviderType.class),
            @Arg(column = "base_url", javaType = String.class),
            @Arg(column = "api_key_ciphertext", javaType = String.class),
            @Arg(column = "api_key_mask", javaType = String.class),
            @Arg(column = "status", javaType = EnableStatus.class),
            @Arg(column = "description", javaType = String.class),
            @Arg(column = "created_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "updated_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class)
    })
    @Select("""
            select id, provider_key, name, provider_type, base_url, api_key_ciphertext, api_key_mask, status, description, created_at, updated_at
            from model_provider
            where id = #{providerId}
            """)
    ModelProviderRecord findById(@Param("providerId") long providerId);

    /**
     * 按业务标识查询模型供应商。
     *
     * @param providerKey 供应商标识
     * @return 供应商记录
     */
    @ConstructorArgs({
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "provider_key", javaType = String.class),
            @Arg(column = "name", javaType = String.class),
            @Arg(column = "provider_type", javaType = ModelProviderType.class),
            @Arg(column = "base_url", javaType = String.class),
            @Arg(column = "api_key_ciphertext", javaType = String.class),
            @Arg(column = "api_key_mask", javaType = String.class),
            @Arg(column = "status", javaType = EnableStatus.class),
            @Arg(column = "description", javaType = String.class),
            @Arg(column = "created_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "updated_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class)
    })
    @Select("""
            select id, provider_key, name, provider_type, base_url, api_key_ciphertext, api_key_mask, status, description, created_at, updated_at
            from model_provider
            where provider_key = #{providerKey}
            """)
    ModelProviderRecord findByProviderKey(@Param("providerKey") String providerKey);

    /**
     * 插入模型供应商。
     *
     * @param record 供应商记录
     * @return 新增后的记录
     */
    @ConstructorArgs({
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "provider_key", javaType = String.class),
            @Arg(column = "name", javaType = String.class),
            @Arg(column = "provider_type", javaType = ModelProviderType.class),
            @Arg(column = "base_url", javaType = String.class),
            @Arg(column = "api_key_ciphertext", javaType = String.class),
            @Arg(column = "api_key_mask", javaType = String.class),
            @Arg(column = "status", javaType = EnableStatus.class),
            @Arg(column = "description", javaType = String.class),
            @Arg(column = "created_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "updated_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class)
    })
    @Select("""
            insert into model_provider(
              provider_key, name, provider_type, base_url, api_key_ciphertext, api_key_mask, status, description
            )
            values (
              #{record.providerKey},
              #{record.name},
              #{record.providerType},
              #{record.baseUrl},
              #{record.apiKeyCiphertext},
              #{record.apiKeyMask},
              #{record.status},
              #{record.description}
            )
            returning id, provider_key, name, provider_type, base_url, api_key_ciphertext, api_key_mask, status, description, created_at, updated_at
            """)
    ModelProviderRecord insert(@Param("record") ModelProviderRecord record);

    /**
     * 更新模型供应商。
     *
     * @param record 供应商记录
     * @return 受影响行数
     */
    @Update("""
            update model_provider
            set name = #{record.name},
                provider_type = #{record.providerType},
                base_url = #{record.baseUrl},
                api_key_ciphertext = #{record.apiKeyCiphertext},
                api_key_mask = #{record.apiKeyMask},
                status = #{record.status},
                description = #{record.description},
                updated_at = now()
            where id = #{record.id}
            """)
    int update(@Param("record") ModelProviderRecord record);

    /**
     * 更新模型供应商状态。
     *
     * @param providerId 供应商主键
     * @param status 状态
     * @return 受影响行数
     */
    @Update("""
            update model_provider
            set status = #{status},
                updated_at = now()
            where id = #{providerId}
            """)
    int updateStatus(@Param("providerId") long providerId, @Param("status") EnableStatus status);
}
