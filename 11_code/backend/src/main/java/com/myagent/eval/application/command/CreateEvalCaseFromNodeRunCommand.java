package com.myagent.eval.application.command;

/**
 * 从 NodeRun 创建验收用例命令。
 *
 * @param nodeRunId NodeRun 数据库主键
 * @param suiteId 验收套件主键
 * @param title 用例标题
 * @param description 用例说明
 */
public record CreateEvalCaseFromNodeRunCommand(
        long nodeRunId,
        long suiteId,
        String title,
        String description
) {
}
