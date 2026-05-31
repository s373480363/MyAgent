package com.myagent.eval.application.query;

import com.myagent.eval.domain.EvalCaseConfirmStatus;

/**
 * 查询验收用例列表参数。
 *
 * @param suiteId 验收套件主键
 * @param page 页码
 * @param pageSize 每页条数
 * @param confirmStatus 确认状态
 * @param critical 是否关键用例
 * @param keyword 关键词
 */
public record ListEvalCasesQuery(
        long suiteId,
        long page,
        long pageSize,
        EvalCaseConfirmStatus confirmStatus,
        Boolean critical,
        String keyword
) {
}
