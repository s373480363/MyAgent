package com.myagent.externalagent.repository;

import com.myagent.common.page.PageResult;
import com.myagent.externalagent.application.query.ListExternalAgentsQuery;

import java.util.Optional;

/**
 * 外部 Agent 仓储接口。
 */
public interface ExternalAgentRepository {

    /**
     * 分页查询外部 Agent。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    PageResult<ExternalAgentRecord> listExternalAgents(ListExternalAgentsQuery query);

    /**
     * 按主键查询外部 Agent。
     *
     * @param adapterId 主键
     * @return 外部 Agent 记录
     */
    Optional<ExternalAgentRecord> findById(long adapterId);

    /**
     * 按适配器标识查询外部 Agent。
     *
     * @param adapterKey 适配器标识
     * @return 外部 Agent 记录
     */
    Optional<ExternalAgentRecord> findByAdapterKey(String adapterKey);

    /**
     * 插入外部 Agent。
     *
     * @param record 外部 Agent 记录
     * @return 新增后的记录
     */
    ExternalAgentRecord insert(ExternalAgentRecord record);

    /**
     * 更新外部 Agent。
     *
     * @param record 外部 Agent 记录
     */
    void update(ExternalAgentRecord record);

    /**
     * 更新外部 Agent 状态。
     *
     * @param adapterId 主键
     * @param status 新状态
     */
    void updateStatus(long adapterId, com.myagent.common.domain.EnableStatus status);
}
