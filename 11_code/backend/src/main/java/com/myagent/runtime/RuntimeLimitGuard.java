package com.myagent.runtime;

/**
 * 运行限制守卫。
 */
public interface RuntimeLimitGuard {

    /**
     * 检查运行总超时。
     *
     * @param context 运行限制上下文
     */
    void checkRunTimeout(RunLimitContext context);

    /**
     * 检查节点超时。
     *
     * @param context 运行限制上下文
     */
    void checkNodeTimeout(RunLimitContext context);

    /**
     * 检查最大步数。
     *
     * @param context 运行限制上下文
     */
    void checkStepLimit(RunLimitContext context);

    /**
     * 检查 Agent 调用深度。
     *
     * @param context 运行限制上下文
     */
    void checkCallDepth(RunLimitContext context);
}
