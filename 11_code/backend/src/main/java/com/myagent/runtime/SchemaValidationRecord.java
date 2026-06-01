package com.myagent.runtime;

import com.myagent.schema.validation.SchemaValidationResult;
import com.myagent.schema.validation.ValidationStage;

/**
 * 节点运行期 Schema 校验记录。
 *
 * @param stage 校验阶段
 * @param result 校验结果
 */
public record SchemaValidationRecord(
        ValidationStage stage,
        SchemaValidationResult result
) {
}
