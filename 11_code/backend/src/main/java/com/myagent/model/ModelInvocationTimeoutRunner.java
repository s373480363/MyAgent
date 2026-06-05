package com.myagent.model;

import com.myagent.common.error.BizException;
import com.myagent.common.error.ErrorCode;
import com.myagent.settings.domain.PlatformSettingsResolver;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 模型调用统一超时执行器。
 */
@Component
public class ModelInvocationTimeoutRunner {

    /**
     * 平台设置读取器。
     */
    private final PlatformSettingsResolver platformSettingsResolver;

    /**
     * 节点执行线程池。
     */
    private final ExecutorService nodeExecutionExecutorService;

    /**
     * 构造模型调用超时执行器。
     *
     * @param platformSettingsResolver 平台设置读取器
     * @param nodeExecutionExecutorService 节点执行线程池
     */
    public ModelInvocationTimeoutRunner(
            PlatformSettingsResolver platformSettingsResolver,
            @Qualifier("nodeExecutionExecutorService") ExecutorService nodeExecutionExecutorService
    ) {
        this.platformSettingsResolver = platformSettingsResolver;
        this.nodeExecutionExecutorService = nodeExecutionExecutorService;
    }

    /**
     * 按平台默认 LLM 超时执行模型调用。
     *
     * @param action 模型调用动作
     * @param timeoutMessage 超时业务消息
     * @param <T> 返回结果类型
     * @return 调用结果
     */
    public <T> T runWithDefaultLlmTimeout(Callable<T> action, String timeoutMessage) {
        Future<T> future = nodeExecutionExecutorService.submit(action);
        long timeoutMillis = TimeUnit.SECONDS.toMillis(platformSettingsResolver.resolveDefaultLlmTimeoutSeconds());
        try {
            // 连接测试与正式 LLM 路径复用同一平台超时契约，避免出现第二套超时真相。
            return future.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException exception) {
            future.cancel(true);
            throw new BizException(ErrorCode.RUN_TIMEOUT, timeoutMessage);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            future.cancel(true);
            throw new BizException(ErrorCode.RUN_TIMEOUT, "模型调用被中断。");
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof BizException bizException) {
                throw bizException;
            }
            // 未知底层异常也要收口成统一中文业务错误，不能把实现细节直接抛给调用方。
            throw new BizException(ErrorCode.NODE_EXECUTION_FAILED, "模型调用失败，请检查模型供应商配置、模型名称或网络连接。");
        }
    }
}
