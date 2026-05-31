package com.myagent.externalagent.application.result;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 外部 Agent 测试结果。
 *
 * @param success 是否执行成功
 * @param status 状态摘要
 * @param exitCode 进程退出码
 * @param httpStatus HTTP 状态码
 * @param stdout stdout 摘要
 * @param stderr stderr 摘要
 * @param outputJson 业务输出 JSON
 * @param summary 业务输出摘要
 * @param errorMessage 中文错误摘要
 * @param durationMs 耗时毫秒
 */
public record ExternalAgentTestResult(
        boolean success,
        String status,
        Integer exitCode,
        Integer httpStatus,
        String stdout,
        String stderr,
        JsonNode outputJson,
        String summary,
        String errorMessage,
        long durationMs
) {
}
