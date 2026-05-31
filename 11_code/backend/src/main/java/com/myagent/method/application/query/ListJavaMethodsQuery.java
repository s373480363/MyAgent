package com.myagent.method.application.query;

import com.myagent.common.domain.EnableStatus;

/**
 * Java 方法列表查询参数。
 *
 * @param page 页码
 * @param pageSize 每页条数
 * @param keyword 关键词
 * @param status 状态过滤
 */
public record ListJavaMethodsQuery(
        long page,
        long pageSize,
        String keyword,
        EnableStatus status
) {
}
