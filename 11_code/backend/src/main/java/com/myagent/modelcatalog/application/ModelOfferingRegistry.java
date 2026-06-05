package com.myagent.modelcatalog.application;

import com.myagent.common.page.PageResult;
import com.myagent.modelcatalog.application.query.ListModelOfferingsQuery;
import com.myagent.modelcatalog.application.result.ModelOfferingDescriptor;

import java.util.List;
import java.util.Optional;

/**
 * 模型供应项安全注册表。
 */
public interface ModelOfferingRegistry {

    /**
     * 分页查询供应项选择列表。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    PageResult<ModelOfferingDescriptor> listEnabled(ListModelOfferingsQuery query);

    /**
     * 按键查询单个供应项。
     *
     * @param offeringKey 供应项标识
     * @return 安全描述
     */
    Optional<ModelOfferingDescriptor> findByOfferingKey(String offeringKey);

    /**
     * 按键批量查询供应项。
     *
     * @param offeringKeys 供应项键列表
     * @return 安全描述列表
     */
    List<ModelOfferingDescriptor> findByOfferingKeys(List<String> offeringKeys);
}
