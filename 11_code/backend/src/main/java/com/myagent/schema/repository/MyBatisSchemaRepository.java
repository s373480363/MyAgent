package com.myagent.schema.repository;

import com.myagent.common.page.PageResult;
import com.myagent.schema.application.query.ListSchemasQuery;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 基于 MyBatis 的 Schema 仓储实现。
 */
@Repository
public class MyBatisSchemaRepository implements SchemaRepository {

    /**
     * Schema Mapper。
     */
    private final SchemaMapper schemaMapper;

    /**
     * 构造 Schema 仓储实现。
     *
     * @param schemaMapper Schema Mapper
     */
    public MyBatisSchemaRepository(SchemaMapper schemaMapper) {
        this.schemaMapper = schemaMapper;
    }

    /**
     * 分页查询 Schema。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    @Override
    public PageResult<SchemaRecord> listSchemas(ListSchemasQuery query) {
        String status = query.status() == null ? null : query.status().getCode();
        String createdFrom = query.createdFrom() == null ? null : query.createdFrom().getCode();
        long total = schemaMapper.countSchemas(query.keyword(), status, createdFrom);
        return PageResult.of(
                schemaMapper.listSchemas(query.keyword(), status, createdFrom, query.pageSize(), offset(query)),
                query.page(),
                query.pageSize(),
                total
        );
    }

    /**
     * 按主键查询 Schema。
     *
     * @param schemaId Schema 主键
     * @return Schema 记录
     */
    @Override
    public Optional<SchemaRecord> findById(long schemaId) {
        return Optional.ofNullable(schemaMapper.findById(schemaId));
    }

    /**
     * 按业务标识和版本查询 Schema。
     *
     * @param schemaKey 业务标识
     * @param version 版本号
     * @return Schema 记录
     */
    @Override
    public Optional<SchemaRecord> findByKeyAndVersion(String schemaKey, int version) {
        return Optional.ofNullable(schemaMapper.findByKeyAndVersion(schemaKey, version));
    }

    /**
     * 查询指定业务标识的最大版本号。
     *
     * @param schemaKey 业务标识
     * @return 最大版本号，不存在时返回 0
     */
    @Override
    public int findMaxVersion(String schemaKey) {
        Integer version = schemaMapper.findMaxVersion(schemaKey);
        return version == null ? 0 : version;
    }

    /**
     * 插入 Schema。
     *
     * @param record Schema 记录
     * @return 插入后的 Schema 记录
     */
    @Override
    public SchemaRecord insert(SchemaRecord record) {
        schemaMapper.insert(record);
        return findByKeyAndVersion(record.getSchemaKey(), record.getVersion()).orElseThrow();
    }

    /**
     * 更新 Schema 草稿。
     *
     * @param record Schema 记录
     * @return 受影响行数
     */
    @Override
    public int updateDraft(SchemaRecord record) {
        return schemaMapper.updateDraft(record);
    }

    /**
     * 锁定 Schema 版本。
     *
     * @param schemaId Schema 主键
     * @return 受影响行数
     */
    @Override
    public int lockSchemaVersion(long schemaId) {
        return schemaMapper.lockSchemaVersion(schemaId);
    }

    /**
     * 计算分页偏移量。
     *
     * @param query 查询条件
     * @return 偏移量
     */
    private long offset(ListSchemasQuery query) {
        return (query.page() - 1L) * query.pageSize();
    }
}
