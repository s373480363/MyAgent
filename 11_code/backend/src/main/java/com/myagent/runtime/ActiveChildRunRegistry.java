package com.myagent.runtime;

import com.myagent.common.error.ErrorCode;
import com.myagent.run.domain.RunStatus;
import com.myagent.run.repository.AgentRunRecord;
import com.myagent.run.repository.AgentRunRepository;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * AGENT_CALL 活跃子运行登记表。
 */
@Component
public class ActiveChildRunRegistry {

    /**
     * AgentRun 仓储。
     */
    private final AgentRunRepository agentRunRepository;

    /**
     * 按父节点运行主键登记的子运行集合。
     */
    private final ConcurrentMap<Long, CopyOnWriteArrayList<AgentRunRecord>> activeChildren = new ConcurrentHashMap<>();

    /**
     * 构造活跃子运行登记表。
     *
     * @param agentRunRepository AgentRun 仓储
     */
    public ActiveChildRunRegistry(AgentRunRepository agentRunRepository) {
        this.agentRunRepository = agentRunRepository;
    }

    /**
     * 登记 AGENT_CALL 创建的子运行。
     *
     * @param parentNodeRunDbId 父节点 NodeRun 主键
     * @param childRun 子运行记录
     */
    public void register(long parentNodeRunDbId, AgentRunRecord childRun) {
        activeChildren.computeIfAbsent(parentNodeRunDbId, ignored -> new CopyOnWriteArrayList<>()).add(childRun);
    }

    /**
     * 取消登记 AGENT_CALL 子运行。
     *
     * @param parentNodeRunDbId 父节点 NodeRun 主键
     * @param childRunId 子运行主键
     */
    public void unregister(long parentNodeRunDbId, long childRunId) {
        CopyOnWriteArrayList<AgentRunRecord> children = activeChildren.get(parentNodeRunDbId);
        if (children == null) {
            return;
        }
        children.removeIf(child -> child.id() == childRunId);
        if (children.isEmpty()) {
            activeChildren.remove(parentNodeRunDbId, children);
        }
    }

    /**
     * 级联取消父节点当前活跃的子运行。
     *
     * @param parentNodeRunDbId 父节点 NodeRun 主键
     * @param reason 中文取消原因
     */
    public void cancelActiveChildren(long parentNodeRunDbId, String reason) {
        List<AgentRunRecord> children = activeChildren.getOrDefault(parentNodeRunDbId, new CopyOnWriteArrayList<>());
        for (AgentRunRecord child : children) {
            long durationMs = child.startedAt() == null ? 0 : Duration.between(child.startedAt(), Instant.now()).toMillis();
            agentRunRepository.cancelActiveRun(
                    child.id(),
                    ErrorCode.RUN_CANCELED.getCode(),
                    reason,
                    Math.max(durationMs, 0)
            );
        }
    }

    /**
     * 判断子运行是否仍是未完成状态。
     *
     * @param runId 子运行主键
     * @return 仍未完成时返回 true
     */
    public boolean isActive(long runId) {
        return agentRunRepository.findById(runId)
                .map(record -> record.status() == RunStatus.PENDING || record.status() == RunStatus.RUNNING)
                .orElse(false);
    }
}
