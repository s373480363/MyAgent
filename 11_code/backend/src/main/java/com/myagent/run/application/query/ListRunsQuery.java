package com.myagent.run.application.query;

import com.myagent.run.domain.RunStatus;
import com.myagent.run.domain.RunType;

import java.time.Instant;

/**
 * 运行列表查询。
 *
 * @param page 页码
 * @param pageSize 每页条数
 * @param agentId Agent 主键
 * @param agentKey Agent 业务标识
 * @param runType 运行类型
 * @param status 运行状态
 * @param keyword 关键词
 * @param startedAtFrom 开始时间下界
 * @param startedAtTo 开始时间上界
 */
public record ListRunsQuery(
        long page,
        long pageSize,
        Long agentId,
        String agentKey,
        RunType runType,
        RunStatus status,
        String keyword,
        Instant startedAtFrom,
        Instant startedAtTo
) {
}
