package com.myagent.externalagent.application.query;

import com.myagent.common.domain.EnableStatus;
import com.myagent.externalagent.domain.ExternalAgentType;

/**
 * 外部 Agent 列表查询参数。
 *
 * @param page 页码
 * @param pageSize 每页条数
 * @param keyword 关键词
 * @param status 状态
 * @param adapterType 适配器类型
 */
public record ListExternalAgentsQuery(
        long page,
        long pageSize,
        String keyword,
        EnableStatus status,
        ExternalAgentType adapterType
) {
}
