package com.myagent.tool.repository;

import com.myagent.common.page.PageResult;
import com.myagent.tool.application.query.ListToolsQuery;

import java.util.Optional;

/**
 * 工具仓储接口。
 */
public interface ToolRepository {

    /**
     * 分页查询工具。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    PageResult<ToolRecord> listTools(ListToolsQuery query);

    /**
     * 按主键查询工具。
     *
     * @param toolId 工具主键
     * @return 工具记录
     */
    Optional<ToolRecord> findById(long toolId);

    /**
     * 按工具标识查询工具。
     *
     * @param toolKey 工具标识
     * @return 工具记录
     */
    Optional<ToolRecord> findByToolKey(String toolKey);
}
