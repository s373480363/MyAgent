package com.myagent.tool.application.query;

import com.myagent.common.domain.EnableStatus;

/**
 * 工具列表查询参数。
 *
 * @param page 页码
 * @param pageSize 每页条数
 * @param keyword 关键词
 * @param status 状态
 * @param executorType 执行器类型
 */
public record ListToolsQuery(
        long page,
        long pageSize,
        String keyword,
        EnableStatus status,
        String executorType
) {
}
