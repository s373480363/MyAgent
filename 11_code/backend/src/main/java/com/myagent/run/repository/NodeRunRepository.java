package com.myagent.run.repository;

import com.myagent.runtime.NodeRunFinishRecord;
import com.myagent.runtime.NodeRunStartRecord;

import java.util.List;
import java.util.Optional;

/**
 * NodeRun 仓储。
 */
public interface NodeRunRepository {

    /**
     * 插入节点运行记录。
     *
     * @param record 节点运行开始记录
     * @return 新增后的节点运行记录
     */
    NodeRunRecord insert(NodeRunStartRecord record);

    /**
     * 完成节点运行记录。
     *
     * @param record 节点运行完成记录
     * @return 受影响行数
     */
    int finish(NodeRunFinishRecord record);

    /**
     * 查询运行下的节点记录。
     *
     * @param runId AgentRun 数据库主键
     * @return 节点运行记录列表
     */
    List<NodeRunRecord> listByRunId(long runId);

    /**
     * 按主键查询节点运行。
     *
     * @param nodeRunId NodeRun 主键
     * @return 节点运行记录
     */
    Optional<NodeRunRecord> findById(long nodeRunId);
}
