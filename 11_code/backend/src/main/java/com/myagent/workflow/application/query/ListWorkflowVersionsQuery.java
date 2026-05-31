package com.myagent.workflow.application.query;

import com.myagent.common.page.PageQuery;
import com.myagent.workflow.domain.WorkflowVersionStatus;

/**
 * 工作流版本列表查询。
 *
 * @param agentId Agent 主键
 * @param page 页码
 * @param pageSize 每页条数
 * @param status 状态过滤
 */
public record ListWorkflowVersionsQuery(
        long agentId,
        long page,
        long pageSize,
        WorkflowVersionStatus status
) {

    /**
     * 返回默认查询。
     *
     * @param agentId Agent 主键
     * @return 默认查询
     */
    public static ListWorkflowVersionsQuery ofDefault(long agentId) {
        return new ListWorkflowVersionsQuery(agentId, PageQuery.DEFAULT_PAGE, PageQuery.DEFAULT_PAGE_SIZE, null);
    }
}
