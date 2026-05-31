package com.myagent.eval.application.query;

/**
 * 查询验收历史对比参数。
 *
 * @param suiteId 验收套件主键
 * @param page 页码
 * @param pageSize 每页条数
 */
public record ListEvalRunHistoryQuery(
        long suiteId,
        long page,
        long pageSize
) {
}
