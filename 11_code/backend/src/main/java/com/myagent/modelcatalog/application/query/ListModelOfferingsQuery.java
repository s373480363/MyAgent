package com.myagent.modelcatalog.application.query;

import com.myagent.common.domain.EnableStatus;

/**
 * 模型供应项分页查询。
 *
 * @param page 页码
 * @param pageSize 每页条数
 * @param providerKey 供应商标识
 * @param keyword 关键词
 * @param status 状态
 */
public record ListModelOfferingsQuery(
        long page,
        long pageSize,
        String providerKey,
        String keyword,
        EnableStatus status
) {
}
