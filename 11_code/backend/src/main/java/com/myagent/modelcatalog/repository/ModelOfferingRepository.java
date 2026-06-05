package com.myagent.modelcatalog.repository;

import com.myagent.common.domain.EnableStatus;
import com.myagent.common.page.PageResult;
import com.myagent.modelcatalog.application.query.ListModelOfferingsQuery;

import java.util.List;
import java.util.Optional;

/**
 * 模型供应项仓储。
 */
public interface ModelOfferingRepository {

    /**
     * 分页查询模型供应项。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    PageResult<ModelOfferingJoinedRecord> listOfferings(ListModelOfferingsQuery query);

    /**
     * 按主键查询模型供应项。
     *
     * @param offeringId 供应项主键
     * @return 供应项记录
     */
    Optional<ModelOfferingRecord> findById(long offeringId);

    /**
     * 按键查询模型供应项。
     *
     * @param offeringKey 供应项标识
     * @return 供应项记录
     */
    Optional<ModelOfferingRecord> findByOfferingKey(String offeringKey);

    /**
     * 按键查询联表视图。
     *
     * @param offeringKey 供应项标识
     * @return 联表记录
     */
    Optional<ModelOfferingJoinedRecord> findJoinedByOfferingKey(String offeringKey);

    /**
     * 按键批量查询联表视图。
     *
     * @param offeringKeys 供应项键列表
     * @return 联表记录列表
     */
    List<ModelOfferingJoinedRecord> findJoinedByOfferingKeys(List<String> offeringKeys);

    /**
     * 按供应商和上游模型名查询。
     *
     * @param providerKey 供应商标识
     * @param upstreamModelName 上游模型名
     * @return 供应项记录
     */
    Optional<ModelOfferingRecord> findByProviderKeyAndUpstreamModelName(String providerKey, String upstreamModelName);

    /**
     * 插入模型供应项。
     *
     * @param record 记录
     * @return 新增后的记录
     */
    ModelOfferingRecord insert(ModelOfferingRecord record);

    /**
     * 更新模型供应项。
     *
     * @param record 记录
     * @return 受影响行数
     */
    int update(ModelOfferingRecord record);

    /**
     * 更新模型供应项状态。
     *
     * @param offeringId 供应项主键
     * @param status 状态
     * @return 受影响行数
     */
    int updateStatus(long offeringId, EnableStatus status);
}
