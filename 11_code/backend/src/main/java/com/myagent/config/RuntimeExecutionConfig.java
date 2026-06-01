package com.myagent.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 运行时执行线程池配置。
 */
@Configuration
public class RuntimeExecutionConfig {

    /**
     * 构造节点执行线程池。
     *
     * @return 节点执行线程池
     */
    @Bean(destroyMethod = "shutdownNow")
    @Qualifier("nodeExecutionExecutorService")
    public ExecutorService nodeExecutionExecutorService() {
        int processors = Math.max(2, Runtime.getRuntime().availableProcessors());
        return new ThreadPoolExecutor(
                processors,
                processors * 4,
                60,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1_000),
                runnable -> {
                    Thread thread = new Thread(runnable);
                    thread.setName("myagent-node-exec-" + thread.threadId());
                    thread.setDaemon(true);
                    return thread;
                },
                new ThreadPoolExecutor.AbortPolicy()
        );
    }
}
