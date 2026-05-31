package com.myagent.eval.application.query;

import com.myagent.run.domain.RunStatus;

import java.time.Instant;

/**
 * 查询验收运行列表参数。
 *
 * @param suiteId 验收套件主键
 * @param page 页码
 * @param pageSize 每页条数
 * @param status 运行状态
 * @param startedAtFrom 开始时间下界
 * @param startedAtTo 开始时间上界
 */
public record ListEvalRunsQuery(
        long suiteId,
        long page,
        long pageSize,
        RunStatus status,
        Instant startedAtFrom,
        Instant startedAtTo
) {
}
