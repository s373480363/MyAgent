package com.myagent.eval.domain;

/**
 * 验收用例确认状态。
 */
public enum EvalCaseConfirmStatus {

    /**
     * 用户创建的正式用例。
     */
    USER_CREATED,

    /**
     * 用户确认后的正式用例。
     */
    USER_CONFIRMED,

    /**
     * AI 或历史运行生成的待确认草稿用例。
     */
    AI_DRAFT_PENDING,

    /**
     * 已归档用例。
     */
    ARCHIVED
}
