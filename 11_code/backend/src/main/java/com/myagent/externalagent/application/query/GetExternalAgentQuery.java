package com.myagent.externalagent.application.query;

/**
 * 外部 Agent 详情查询参数。
 *
 * @param adapterId 外部 Agent 主键
 */
public record GetExternalAgentQuery(long adapterId) {
}
