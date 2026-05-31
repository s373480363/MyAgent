package com.myagent.externalagent.application.command;

import java.util.List;

/**
 * 更新外部 Agent 敏感 secret 命令。
 *
 * @param adapterId 外部 Agent 主键
 * @param items 覆盖写入项
 * @param clearHeaderNames 显式清空的 header 名称列表
 */
public record UpdateExternalAgentSecretsCommand(
        long adapterId,
        List<Item> items,
        List<String> clearHeaderNames
) {

    /**
     * 单个 secret 写入项。
     *
     * @param headerName header 名称
     * @param secretValue secret 值
     */
    public record Item(String headerName, String secretValue) {
    }
}
