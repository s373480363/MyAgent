package com.myagent.tool.runtime;

/**
 * 工具注册目录。
 */
public interface ToolRegistry {

    /**
     * 刷新工具执行器目录。
     */
    void refresh();

    /**
     * 获取已启用工具描述。
     *
     * @param toolKey 工具业务标识
     * @return 工具运行时描述
     */
    ToolDescriptor getEnabledTool(String toolKey);
}
