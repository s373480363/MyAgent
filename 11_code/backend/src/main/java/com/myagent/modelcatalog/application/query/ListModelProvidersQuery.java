package com.myagent.modelcatalog.application.query;

import com.myagent.common.domain.EnableStatus;

/**
 * 模型供应商分页查询。
 *
 * @param page 页码
 * @param pageSize 每页条数
 * @param keyword 关键词
 * @param status 状态
 */
public record ListModelProvidersQuery(
        long page,
        long pageSize,
        String keyword,
        EnableStatus status
) {
}
