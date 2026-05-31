package com.myagent.externalagent.repository;

import com.myagent.common.domain.EnableStatus;
import com.myagent.common.page.PageResult;
import com.myagent.externalagent.application.query.ListExternalAgentsQuery;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 基于 MyBatis 的外部 Agent 仓储实现。
 */
@Repository
public class MyBatisExternalAgentRepository implements ExternalAgentRepository {

    /**
     * MyBatis Mapper。
     */
    private final ExternalAgentMapper externalAgentMapper;

    /**
     * 构造外部 Agent 仓储。
     *
     * @param externalAgentMapper MyBatis Mapper
     */
    public MyBatisExternalAgentRepository(ExternalAgentMapper externalAgentMapper) {
        this.externalAgentMapper = externalAgentMapper;
    }

    /**
     * 分页查询外部 Agent。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    @Override
    public PageResult<ExternalAgentRecord> listExternalAgents(ListExternalAgentsQuery query) {
        long offset = (query.page() - 1) * query.pageSize();
        return PageResult.of(
                externalAgentMapper.listExternalAgents(query.keyword(), query.status(), query.adapterType(), query.pageSize(), offset),
                query.page(),
                query.pageSize(),
                externalAgentMapper.countExternalAgents(query.keyword(), query.status(), query.adapterType())
        );
    }

    /**
     * 按主键查询外部 Agent。
     *
     * @param adapterId 主键
     * @return 外部 Agent 记录
     */
    @Override
    public Optional<ExternalAgentRecord> findById(long adapterId) {
        return Optional.ofNullable(externalAgentMapper.findById(adapterId));
    }

    /**
     * 按适配器标识查询外部 Agent。
     *
     * @param adapterKey 适配器标识
     * @return 外部 Agent 记录
     */
    @Override
    public Optional<ExternalAgentRecord> findByAdapterKey(String adapterKey) {
        return Optional.ofNullable(externalAgentMapper.findByAdapterKey(adapterKey));
    }

    /**
     * 插入外部 Agent。
     *
     * @param record 外部 Agent 记录
     * @return 新增后的记录
     */
    @Override
    public ExternalAgentRecord insert(ExternalAgentRecord record) {
        return externalAgentMapper.insert(record);
    }

    /**
     * 更新外部 Agent。
     *
     * @param record 外部 Agent 记录
     */
    @Override
    public void update(ExternalAgentRecord record) {
        externalAgentMapper.update(record);
    }

    /**
     * 更新外部 Agent 状态。
     *
     * @param adapterId 主键
     * @param status 新状态
     */
    @Override
    public void updateStatus(long adapterId, EnableStatus status) {
        externalAgentMapper.updateStatus(adapterId, status);
    }
}
