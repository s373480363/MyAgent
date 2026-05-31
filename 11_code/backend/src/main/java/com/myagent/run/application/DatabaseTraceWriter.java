package com.myagent.run.application;

import com.myagent.run.repository.NodeRunRecord;
import com.myagent.run.repository.NodeRunRepository;
import com.myagent.run.repository.TraceEventRepository;
import com.myagent.runtime.NodeRunFinishRecord;
import com.myagent.runtime.NodeRunStartRecord;
import com.myagent.runtime.NodeRunStartResult;
import com.myagent.runtime.TraceEventRecord;
import com.myagent.runtime.TraceWriter;
import org.springframework.stereotype.Component;

/**
 * 基于数据库的 Trace 写入器。
 */
@Component
public class DatabaseTraceWriter implements TraceWriter {

    /**
     * NodeRun 仓储。
     */
    private final NodeRunRepository nodeRunRepository;

    /**
     * TraceEvent 仓储。
     */
    private final TraceEventRepository traceEventRepository;

    /**
     * 构造 Trace 写入器。
     *
     * @param nodeRunRepository NodeRun 仓储
     * @param traceEventRepository TraceEvent 仓储
     */
    public DatabaseTraceWriter(
            NodeRunRepository nodeRunRepository,
            TraceEventRepository traceEventRepository
    ) {
        this.nodeRunRepository = nodeRunRepository;
        this.traceEventRepository = traceEventRepository;
    }

    /**
     * 创建节点运行记录。
     *
     * @param record 节点运行开始记录
     * @return 节点运行开始结果
     */
    @Override
    public NodeRunStartResult createNodeRun(NodeRunStartRecord record) {
        NodeRunRecord inserted = nodeRunRepository.insert(record);
        return new NodeRunStartResult(
                inserted.id(),
                inserted.runId(),
                record.agentRunNo(),
                inserted.nodeId(),
                inserted.startedAt()
        );
    }

    /**
     * 完成节点运行记录。
     *
     * @param record 节点运行完成记录
     */
    @Override
    public void finishNodeRun(NodeRunFinishRecord record) {
        nodeRunRepository.finish(record);
    }

    /**
     * 写入 Trace 事件。
     *
     * @param record Trace 事件记录
     */
    @Override
    public void writeEvent(TraceEventRecord record) {
        traceEventRepository.insert(record);
    }
}
