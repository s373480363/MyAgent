package com.myagent.modelcatalog.repository;

import com.myagent.common.domain.EnableStatus;
import com.myagent.common.page.PageResult;
import com.myagent.modelcatalog.application.query.ListModelProvidersQuery;

import java.util.Optional;

/**
 * 模型供应商仓储。
 */
public interface ModelProviderRepository {

    /**
     * 分页查询模型供应商。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    PageResult<ModelProviderRecord> listProviders(ListModelProvidersQuery query);

    /**
     * 按主键查询模型供应商。
     *
     * @param providerId 供应商主键
     * @return 供应商记录
     */
    Optional<ModelProviderRecord> findById(long providerId);

    /**
     * 按业务标识查询模型供应商。
     *
     * @param providerKey 供应商标识
     * @return 供应商记录
     */
    Optional<ModelProviderRecord> findByProviderKey(String providerKey);

    /**
     * 插入模型供应商。
     *
     * @param record 记录
     * @return 新增后的记录
     */
    ModelProviderRecord insert(ModelProviderRecord record);

    /**
     * 更新模型供应商普通字段。
     *
     * @param record 记录
     * @return 受影响行数
     */
    int update(ModelProviderRecord record);

    /**
     * 更新模型供应商状态。
     *
     * @param providerId 供应商主键
     * @param status 状态
     * @return 受影响行数
     */
    int updateStatus(long providerId, EnableStatus status);
}
