package com.myagent.schema.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Schema MyBatis Mapper。
 */
@Mapper
public interface SchemaMapper {

    /**
     * 分页查询 Schema。
     *
     * @param keyword 关键词
     * @param status 生命周期状态
     * @param createdFrom 来源
     * @param limit 返回条数
     * @param offset 偏移量
     * @return Schema 记录列表
     */
    List<SchemaRecord> listSchemas(
            @Param("keyword") String keyword,
            @Param("status") String status,
            @Param("createdFrom") String createdFrom,
            @Param("limit") long limit,
            @Param("offset") long offset
    );

    /**
     * 统计 Schema 数量。
     *
     * @param keyword 关键词
     * @param status 生命周期状态
     * @param createdFrom 来源
     * @return 总数
     */
    long countSchemas(
            @Param("keyword") String keyword,
            @Param("status") String status,
            @Param("createdFrom") String createdFrom
    );

    /**
     * 按主键查询 Schema。
     *
     * @param schemaId Schema 主键
     * @return Schema 记录
     */
    SchemaRecord findById(@Param("schemaId") long schemaId);

    /**
     * 按业务标识和版本查询 Schema。
     *
     * @param schemaKey 业务标识
     * @param version 版本号
     * @return Schema 记录
     */
    SchemaRecord findByKeyAndVersion(@Param("schemaKey") String schemaKey, @Param("version") int version);

    /**
     * 查询指定业务标识的最大版本号。
     *
     * @param schemaKey 业务标识
     * @return 最大版本号
     */
    Integer findMaxVersion(@Param("schemaKey") String schemaKey);

    /**
     * 插入 Schema。
     *
     * @param record Schema 记录
     * @return 受影响行数
     */
    int insert(@Param("record") SchemaRecord record);

    /**
     * 更新 Schema 草稿。
     *
     * @param record Schema 记录
     * @return 受影响行数
     */
    int updateDraft(@Param("record") SchemaRecord record);

    /**
     * 锁定 Schema 版本。
     *
     * @param schemaId Schema 主键
     * @return 受影响行数
     */
    int lockSchemaVersion(@Param("schemaId") long schemaId);
}
