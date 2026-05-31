package com.myagent.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.myagent.run.domain.RunStatus;

/**
 * 节点运行完成记录。
 *
 * @param nodeRunDbId NodeRun 数据库主键
 * @param status 节点状态
 * @param outputJson 节点输出
 * @param schemaValidationResultJson Schema 校验结果
 * @param errorMessage 错误消息
 * @param durationMs 执行耗时
 */
public record NodeRunFinishRecord(
        long nodeRunDbId,
        RunStatus status,
        JsonNode outputJson,
        JsonNode schemaValidationResultJson,
        String errorMessage,
        Long durationMs
) {
}
