package com.myagent.eval.application.command;

import java.util.List;

/**
 * 运行验收套件命令。
 *
 * @param suiteId 验收套件主键
 * @param caseIds 指定运行的用例主键列表；为空时运行全部正式用例
 */
public record RunEvalSuiteCommand(
        long suiteId,
        List<Long> caseIds
) {
}
