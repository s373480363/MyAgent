package com.myagent.tool.runtime;

import com.myagent.tool.repository.ToolRecord;

/**
 * 工具运行时描述。
 *
 * @param record 工具主数据
 * @param executor 工具执行器
 */
public record ToolDescriptor(
        ToolRecord record,
        ToolExecutor executor
) {
}
