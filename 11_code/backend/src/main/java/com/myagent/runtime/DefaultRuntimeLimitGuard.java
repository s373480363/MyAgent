package com.myagent.runtime;

import com.myagent.common.error.BizException;
import com.myagent.common.error.ErrorCode;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * 默认运行限制守卫。
 */
@Component
public class DefaultRuntimeLimitGuard implements RuntimeLimitGuard {

    /**
     * 检查运行总超时。
     *
     * @param context 运行限制上下文
     */
    @Override
    public void checkRunTimeout(RunLimitContext context) {
        if (context.startedAt() == null || context.runtimeOptions() == null) {
            return;
        }
        long elapsedSeconds = Duration.between(context.startedAt(), Instant.now()).toSeconds();
        if (elapsedSeconds > context.runtimeOptions().getTimeoutSeconds()) {
            throw new BizException(ErrorCode.RUN_TIMEOUT, "运行超过工作流版本总超时。");
        }
    }

    /**
     * 检查节点超时。
     *
     * @param context 运行限制上下文
     */
    @Override
    public void checkNodeTimeout(RunLimitContext context) {
        if (context.nodeStartedAt() == null || context.nodeTimeoutSeconds() == null) {
            return;
        }
        long elapsedSeconds = Duration.between(context.nodeStartedAt(), Instant.now()).toSeconds();
        if (elapsedSeconds > context.nodeTimeoutSeconds()) {
            throw new BizException(ErrorCode.RUN_TIMEOUT, "节点执行超过节点级超时。");
        }
    }

    /**
     * 检查最大步数。
     *
     * @param context 运行限制上下文
     */
    @Override
    public void checkStepLimit(RunLimitContext context) {
        if (context.runtimeOptions() == null) {
            return;
        }
        if (context.currentStep() > context.runtimeOptions().getMaxSteps()) {
            throw new BizException(ErrorCode.RUN_TIMEOUT, "运行超过最大执行步数。");
        }
    }

    /**
     * 检查 Agent 调用深度。
     *
     * @param context 运行限制上下文
     */
    @Override
    public void checkCallDepth(RunLimitContext context) {
        if (context.runtimeOptions() == null) {
            return;
        }
        if (context.currentCallDepth() > context.runtimeOptions().getMaxAgentCallDepth()) {
            throw new BizException(ErrorCode.RUN_TIMEOUT, "运行超过最大 Agent 调用深度。");
        }
    }
}
