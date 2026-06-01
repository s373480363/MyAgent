package com.myagent.tool.application;

import com.myagent.common.error.BizException;
import com.myagent.common.error.ErrorCode;
import com.myagent.common.page.PageResult;
import com.myagent.tool.application.query.GetToolQuery;
import com.myagent.tool.application.query.ListToolsQuery;
import com.myagent.tool.application.result.ToolDetailResult;
import com.myagent.tool.application.result.ToolListItemResult;
import com.myagent.tool.repository.ToolRecord;
import com.myagent.tool.repository.ToolRepository;
import com.myagent.tool.runtime.ToolRegistry;
import org.springframework.stereotype.Service;

/**
 * 工具应用服务默认实现。
 */
@Service
public class DefaultToolApplicationService implements ToolApplicationService {

    /**
     * 工具仓储。
     */
    private final ToolRepository toolRepository;

    /**
     * 工具注册目录。
     */
    private final ToolRegistry toolRegistry;

    /**
     * 构造工具应用服务。
     *
     * @param toolRepository 工具仓储
     * @param toolRegistry 工具注册目录
     */
    public DefaultToolApplicationService(ToolRepository toolRepository, ToolRegistry toolRegistry) {
        this.toolRepository = toolRepository;
        this.toolRegistry = toolRegistry;
    }

    /**
     * 查询工具分页列表。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    @Override
    public PageResult<ToolListItemResult> listTools(ListToolsQuery query) {
        return toolRepository.listTools(query).map(this::toListItem);
    }

    /**
     * 查询工具详情。
     *
     * @param query 查询条件
     * @return 详情结果
     */
    @Override
    public ToolDetailResult getTool(GetToolQuery query) {
        ToolRecord record = toolRepository.findById(query.toolId())
                .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND, "指定工具不存在。"));
        return toDetail(record);
    }

    /**
     * 刷新工具目录。
     */
    @Override
    public void refreshToolCatalog() {
        toolRegistry.refresh();
    }

    /**
     * 转换列表项结果。
     *
     * @param record 持久化记录
     * @return 列表项结果
     */
    private ToolListItemResult toListItem(ToolRecord record) {
        return new ToolListItemResult(
                record.id(),
                record.toolKey(),
                record.name(),
                record.description(),
                record.inputSchemaId(),
                record.outputSchemaId(),
                record.executorType(),
                record.executorConfigJson(),
                record.status(),
                record.createdAt(),
                record.updatedAt()
        );
    }

    /**
     * 转换详情结果。
     *
     * @param record 持久化记录
     * @return 详情结果
     */
    private ToolDetailResult toDetail(ToolRecord record) {
        return new ToolDetailResult(
                record.id(),
                record.toolKey(),
                record.name(),
                record.description(),
                record.inputSchemaId(),
                record.outputSchemaId(),
                record.executorType(),
                record.executorConfigJson(),
                record.status(),
                record.createdAt(),
                record.updatedAt()
        );
    }
}
