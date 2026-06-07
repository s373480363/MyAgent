package com.myagent.eval.application.result;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 单条 hardCheck 结果。
 *
 * @param type hardCheck 类型
 * @param passed 是否通过
 * @param message 中文结果说明
 * @param path 对应 JSONPath
 * @param expected 期望值
 * @param actual 实际值
 * @param details 补充排障信息
 */
public record EvalHardCheckResultItem(
        String type,
        boolean passed,
        String message,
        String path,
        JsonNode expected,
        JsonNode actual,
        JsonNode details
) {
}
