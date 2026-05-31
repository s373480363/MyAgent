package com.myagent.runtime;

/**
 * Trace 写入器。
 */
public interface TraceWriter {

    /**
     * 创建节点运行记录。
     *
     * @param record 节点运行开始记录
     * @return 节点运行开始结果
     */
    NodeRunStartResult createNodeRun(NodeRunStartRecord record);

    /**
     * 完成节点运行记录。
     *
     * @param record 节点运行完成记录
     */
    void finishNodeRun(NodeRunFinishRecord record);

    /**
     * 写入 Trace 事件。
     *
     * @param record Trace 事件记录
     */
    void writeEvent(TraceEventRecord record);
}
