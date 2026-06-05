package com.myagent.modelcatalog.application.result;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 按键批量查询模型供应项结果。
 *
 * @param items 已找到的供应项
 * @param missingKeys 未找到的供应项键
 */
@Schema(name = "ModelOfferingBatchResult", description = "按键批量查询模型供应项结果。")
public record ModelOfferingBatchResult(
        List<ModelOfferingDescriptor> items,
        List<String> missingKeys
) {
}
