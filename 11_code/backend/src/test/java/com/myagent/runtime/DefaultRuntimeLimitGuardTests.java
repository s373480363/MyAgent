package com.myagent.runtime;

import com.myagent.common.error.BizException;
import com.myagent.workflow.domain.WorkflowRuntimeOptions;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 默认运行限制守卫测试。
 */
class DefaultRuntimeLimitGuardTests {

    /**
     * 运行限制守卫。
     */
    private final DefaultRuntimeLimitGuard guard = new DefaultRuntimeLimitGuard();

    /**
     * 超过工作流版本总超时时必须失败。
     */
    @Test
    void checkRunTimeoutFailsWhenElapsedExceedsSnapshotLimit() {
        RunLimitContext context = new RunLimitContext(
                1L,
                "run_1",
                Instant.now().minusSeconds(11),
                null,
                1,
                0,
                null,
                new WorkflowRuntimeOptions(10, 30, 3)
        );

        assertThatThrownBy(() -> guard.checkRunTimeout(context))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("总超时");
    }

    /**
     * 超过最大步数时必须失败。
     */
    @Test
    void checkStepLimitFailsWhenStepExceedsSnapshotLimit() {
        RunLimitContext context = new RunLimitContext(
                1L,
                "run_1",
                Instant.now(),
                null,
                31,
                0,
                null,
                new WorkflowRuntimeOptions(600, 30, 3)
        );

        assertThatThrownBy(() -> guard.checkStepLimit(context))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("最大执行步数");
    }
}
