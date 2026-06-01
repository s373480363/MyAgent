package com.myagent.tool.runtime;

import com.myagent.common.domain.EnableStatus;
import com.myagent.common.error.BizException;
import com.myagent.common.error.ErrorCode;
import com.myagent.tool.repository.ToolRecord;
import com.myagent.tool.repository.ToolRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认工具注册目录。
 */
@Component
public class DefaultToolRegistry implements ToolRegistry {

    /**
     * 工具仓储。
     */
    private final ToolRepository toolRepository;

    /**
     * Spring 中声明的工具执行器。
     */
    private final List<ToolExecutor> toolExecutors;

    /**
     * 执行器类型索引。
     */
    private final Map<String, ToolExecutor> executorsByType = new ConcurrentHashMap<>();

    /**
     * 构造工具注册目录。
     *
     * @param toolRepository 工具仓储
     * @param toolExecutors 工具执行器列表
     */
    public DefaultToolRegistry(ToolRepository toolRepository, List<ToolExecutor> toolExecutors) {
        this.toolRepository = toolRepository;
        this.toolExecutors = toolExecutors;
        refresh();
    }

    /**
     * 刷新工具执行器目录。
     */
    @Override
    public final void refresh() {
        Map<String, ToolExecutor> refreshed = new ConcurrentHashMap<>();
        for (ToolExecutor executor : toolExecutors) {
            if (executor.executorType() == null || executor.executorType().isBlank()) {
                throw new BizException(ErrorCode.TOOL_CALL_FAILED, "工具执行器类型不能为空。");
            }
            ToolExecutor previous = refreshed.putIfAbsent(executor.executorType(), executor);
            if (previous != null) {
                throw new BizException(ErrorCode.TOOL_CALL_FAILED, "工具执行器重复注册：" + executor.executorType());
            }
        }
        executorsByType.clear();
        executorsByType.putAll(refreshed);
    }

    /**
     * 获取已启用工具描述。
     *
     * @param toolKey 工具业务标识
     * @return 工具运行时描述
     */
    @Override
    public ToolDescriptor getEnabledTool(String toolKey) {
        ToolRecord tool = toolRepository.findByToolKey(toolKey)
                .orElseThrow(() -> new BizException(ErrorCode.TOOL_CALL_FAILED, "工具不存在：" + toolKey));
        if (tool.status() != EnableStatus.ENABLED) {
            throw new BizException(ErrorCode.TOOL_CALL_FAILED, "工具已停用：" + toolKey);
        }
        ToolExecutor executor = executorsByType.get(tool.executorType());
        if (executor == null) {
            throw new BizException(ErrorCode.TOOL_CALL_FAILED, "工具执行器未注册：" + tool.executorType());
        }
        return new ToolDescriptor(tool, executor);
    }
}
