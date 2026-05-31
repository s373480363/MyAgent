package com.myagent.run.repository;

import com.myagent.runtime.TraceEventRecord;

import java.util.List;

/**
 * TraceEvent 仓储。
 */
public interface TraceEventRepository {

    /**
     * 插入 Trace 事件。
     *
     * @param record Trace 事件记录
     * @return 新增后的记录
     */
    RunTraceEventRecord insert(TraceEventRecord record);

    /**
     * 查询运行下的 Trace 事件。
     *
     * @param runId AgentRun 数据库主键
     * @return Trace 事件列表
     */
    List<RunTraceEventRecord> listByRunId(long runId);
}
