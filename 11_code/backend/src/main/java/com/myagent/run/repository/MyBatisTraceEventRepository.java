package com.myagent.run.repository;

import com.myagent.runtime.TraceEventRecord;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 基于 MyBatis 的 TraceEvent 仓储实现。
 */
@Repository
public class MyBatisTraceEventRepository implements TraceEventRepository {

    /**
     * TraceEvent Mapper。
     */
    private final TraceEventMapper traceEventMapper;

    /**
     * 构造 TraceEvent 仓储。
     *
     * @param traceEventMapper TraceEvent Mapper
     */
    public MyBatisTraceEventRepository(TraceEventMapper traceEventMapper) {
        this.traceEventMapper = traceEventMapper;
    }

    /**
     * 插入 Trace 事件。
     *
     * @param record Trace 事件记录
     * @return 新增后的记录
     */
    @Override
    public RunTraceEventRecord insert(TraceEventRecord record) {
        traceEventMapper.insert(record);
        return traceEventMapper.findLatest(record.agentRunDbId(), record.nodeRunDbId(), record.eventType());
    }

    /**
     * 查询运行下的 Trace 事件。
     *
     * @param runId AgentRun 数据库主键
     * @return Trace 事件列表
     */
    @Override
    public List<RunTraceEventRecord> listByRunId(long runId) {
        return traceEventMapper.listByRunId(runId);
    }
}
