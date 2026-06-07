package com.myagent.eval.web.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

/**
 * 更新验收套件请求。
 *
 * @param name 套件名称
 * @param goal 验收目标
 * @param passThreshold 通过率阈值
 */
public record UpdateEvalSuiteRequest(
        @NotBlank String name,
        String goal,
        @NotBlank String judgeModelOfferingKey,
        @DecimalMin("0") @DecimalMax("2") BigDecimal judgeTemperature,
        @DecimalMin("0") @DecimalMax("100") BigDecimal passThreshold
) {
}
