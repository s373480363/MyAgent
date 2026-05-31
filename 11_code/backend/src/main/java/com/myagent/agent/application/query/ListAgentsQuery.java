package com.myagent.agent.application.query;

import com.myagent.common.domain.EnableStatus;
import com.myagent.common.page.PageQuery;

/**
 * Agent 列表查询。
 *
 * @param page 页码
 * @param pageSize 每页条数
 * @param keyword 关键词
 * @param status 状态
 */
public record ListAgentsQuery(
        long page,
        long pageSize,
        String keyword,
        EnableStatus status
) {

    /**
     * 返回默认查询。
     *
     * @return 默认分页查询
     */
    public static ListAgentsQuery ofDefault() {
        return new ListAgentsQuery(PageQuery.DEFAULT_PAGE, PageQuery.DEFAULT_PAGE_SIZE, null, null);
    }
}
