package com.myagent.tool.application;

import com.myagent.common.page.PageResult;
import com.myagent.tool.application.query.GetToolQuery;
import com.myagent.tool.application.query.ListToolsQuery;
import com.myagent.tool.application.result.ToolDetailResult;
import com.myagent.tool.application.result.ToolListItemResult;

/**
 * 工具应用服务。
 */
public interface ToolApplicationService {

    /**
     * 查询工具分页列表。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    PageResult<ToolListItemResult> listTools(ListToolsQuery query);

    /**
     * 查询工具详情。
     *
     * @param query 查询条件
     * @return 详情结果
     */
    ToolDetailResult getTool(GetToolQuery query);

    /**
     * 刷新工具目录。
     */
    void refreshToolCatalog();
}
