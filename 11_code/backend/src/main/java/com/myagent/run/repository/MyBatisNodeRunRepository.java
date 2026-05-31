package com.myagent.run.repository;

import com.myagent.runtime.NodeRunFinishRecord;
import com.myagent.runtime.NodeRunStartRecord;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 基于 MyBatis 的 NodeRun 仓储实现。
 */
@Repository
public class MyBatisNodeRunRepository implements NodeRunRepository {

    /**
     * NodeRun Mapper。
     */
    private final NodeRunMapper nodeRunMapper;

    /**
     * 构造 NodeRun 仓储。
     *
     * @param nodeRunMapper NodeRun Mapper
     */
    public MyBatisNodeRunRepository(NodeRunMapper nodeRunMapper) {
        this.nodeRunMapper = nodeRunMapper;
    }

    /**
     * 插入节点运行记录。
     *
     * @param record 节点运行开始记录
     * @return 新增后的节点运行记录
     */
    @Override
    public NodeRunRecord insert(NodeRunStartRecord record) {
        nodeRunMapper.insert(record);
        return nodeRunMapper.findLatestByRunAndNode(record.agentRunDbId(), record.nodeId());
    }

    /**
     * 完成节点运行记录。
     *
     * @param record 节点运行完成记录
     * @return 受影响行数
     */
    @Override
    public int finish(NodeRunFinishRecord record) {
        return nodeRunMapper.finish(record);
    }

    /**
     * 查询运行下的节点记录。
     *
     * @param runId AgentRun 数据库主键
     * @return 节点运行记录列表
     */
    @Override
    public List<NodeRunRecord> listByRunId(long runId) {
        return nodeRunMapper.listByRunId(runId);
    }

    /**
     * 按主键查询节点运行。
     *
     * @param nodeRunId NodeRun 主键
     * @return 节点运行记录
     */
    @Override
    public Optional<NodeRunRecord> findById(long nodeRunId) {
        return Optional.ofNullable(nodeRunMapper.findById(nodeRunId));
    }
}
