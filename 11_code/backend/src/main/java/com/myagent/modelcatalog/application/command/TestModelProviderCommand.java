package com.myagent.modelcatalog.application.command;

/**
 * 测试模型供应商连接命令。
 *
 * @param providerId 供应商主键
 * @param offeringKey 供应项标识
 * @param prompt 测试提示词
 */
public record TestModelProviderCommand(
        long providerId,
        String offeringKey,
        String prompt
) {
}
