package com.myagent.eval.domain;

/**
 * 验收套件状态。
 */
public enum EvalSuiteStatus {

    /**
     * 草稿态，可编辑。
     */
    DRAFT,

    /**
     * 已确认，可执行正式验收。
     */
    CONFIRMED,

    /**
     * 已归档，不再执行新验收。
     */
    ARCHIVED
}
