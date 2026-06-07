package com.myagent.eval.application.command;

import java.math.BigDecimal;

/**
 * 更新验收套件命令。
 *
 * @param suiteId 验收套件主键
 * @param name 套件名称
 * @param goal 验收目标
 * @param passThreshold 通过率阈值
 */
public record UpdateEvalSuiteCommand(
        long suiteId,
        String name,
        String goal,
        String judgeModelOfferingKey,
        BigDecimal judgeTemperature,
        BigDecimal passThreshold
) {
}
