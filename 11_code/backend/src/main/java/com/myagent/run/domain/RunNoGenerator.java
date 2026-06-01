package com.myagent.run.domain;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 运行编号生成器，集中维护 AgentRun 与 EvalRun 的对外编号格式。
 */
@Component
public class RunNoGenerator {

    /**
     * 编号时间格式。
     */
    private static final DateTimeFormatter NO_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            .withZone(ZoneId.of("Asia/Shanghai"));

    /**
     * 生成 AgentRun 对外编号。
     *
     * @return AgentRun 对外编号
     */
    public String nextRunNo() {
        return next("run");
    }

    /**
     * 生成 EvalRun 对外编号。
     *
     * @return EvalRun 对外编号
     */
    public String nextEvalRunNo() {
        return next("eval");
    }

    /**
     * 按指定前缀生成运行类编号。
     *
     * @param prefix 编号前缀
     * @return 运行类编号
     */
    private String next(String prefix) {
        return prefix + "_" + NO_TIME_FORMATTER.format(Instant.now()) + "_" + UUID.randomUUID().toString().substring(0, 8);
    }
}
