package com.myagent.schema.repository;

import com.myagent.common.page.PageResult;
import com.myagent.schema.application.query.ListSchemasQuery;

import java.util.Optional;

/**
 * Schema 仓储接口。
 */
public interface SchemaRepository {

    /**
     * 分页查询 Schema。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    PageResult<SchemaRecord> listSchemas(ListSchemasQuery query);

    /**
     * 按主键查询 Schema。
     *
     * @param schemaId Schema 主键
     * @return Schema 记录
     */
    Optional<SchemaRecord> findById(long schemaId);

    /**
     * 按业务标识和版本查询 Schema。
     *
     * @param schemaKey 业务标识
     * @param version 版本号
     * @return Schema 记录
     */
    Optional<SchemaRecord> findByKeyAndVersion(String schemaKey, int version);

    /**
     * 查询指定业务标识的最大版本号。
     *
     * @param schemaKey 业务标识
     * @return 最大版本号，不存在时返回 0
     */
    int findMaxVersion(String schemaKey);

    /**
     * 插入 Schema。
     *
     * @param record Schema 记录
     * @return 插入后的 Schema 记录
     */
    SchemaRecord insert(SchemaRecord record);

    /**
     * 更新 Schema 草稿。
     *
     * @param record Schema 记录
     * @return 受影响行数
     */
    int updateDraft(SchemaRecord record);

    /**
     * 锁定 Schema 版本。
     *
     * @param schemaId Schema 主键
     * @return 受影响行数
     */
    int lockSchemaVersion(long schemaId);
}
