package com.myagent.tool.repository;

import com.myagent.common.page.PageResult;
import com.myagent.tool.application.query.ListToolsQuery;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 基于 MyBatis 的工具仓储实现。
 */
@Repository
public class MyBatisToolRepository implements ToolRepository {

    /**
     * MyBatis Mapper。
     */
    private final ToolMapper toolMapper;

    /**
     * 构造工具仓储。
     *
     * @param toolMapper MyBatis Mapper
     */
    public MyBatisToolRepository(ToolMapper toolMapper) {
        this.toolMapper = toolMapper;
    }

    /**
     * 分页查询工具。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    @Override
    public PageResult<ToolRecord> listTools(ListToolsQuery query) {
        long offset = (query.page() - 1) * query.pageSize();
        return PageResult.of(
                toolMapper.listTools(query.keyword(), query.status(), query.executorType(), query.pageSize(), offset),
                query.page(),
                query.pageSize(),
                toolMapper.countTools(query.keyword(), query.status(), query.executorType())
        );
    }

    /**
     * 按主键查询工具。
     *
     * @param toolId 工具主键
     * @return 工具记录
     */
    @Override
    public Optional<ToolRecord> findById(long toolId) {
        return Optional.ofNullable(toolMapper.findById(toolId));
    }

    /**
     * 按工具标识查询工具。
     *
     * @param toolKey 工具标识
     * @return 工具记录
     */
    @Override
    public Optional<ToolRecord> findByToolKey(String toolKey) {
        return Optional.ofNullable(toolMapper.findByToolKey(toolKey));
    }
}
