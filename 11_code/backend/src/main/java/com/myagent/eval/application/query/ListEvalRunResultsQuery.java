package com.myagent.eval.application.query;

/**
 * 查询验收运行结果明细参数。
 *
 * @param evalRunId 对外验收运行编号
 * @param page 页码
 * @param pageSize 每页条数
 * @param passed 是否通过
 * @param critical 是否关键用例
 * @param keyword 关键词
 */
public record ListEvalRunResultsQuery(
        String evalRunId,
        long page,
        long pageSize,
        Boolean passed,
        Boolean critical,
        String keyword
) {
}
