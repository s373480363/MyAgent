package com.myagent.eval.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.myagent.agent.repository.AgentRecord;
import com.myagent.eval.repository.EvalCaseRecord;
import com.myagent.eval.repository.EvalCaseResultRecord;
import com.myagent.eval.repository.EvalCaseResultRepository;
import com.myagent.eval.repository.EvalRunRecord;
import com.myagent.eval.repository.EvalRunRepository;
import com.myagent.eval.repository.EvalSuiteRecord;
import com.myagent.run.domain.RunStatus;
import com.myagent.run.domain.RunType;
import com.myagent.run.repository.AgentRunRecord;
import com.myagent.run.repository.AgentRunRepository;
import com.myagent.workflow.repository.WorkflowVersionRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * EvalRun 生命周期持久化服务，保证运行审计记录按小事务提交。
 */
@Service
public class EvalRunLifecycleService {

    /**
     * JSON 对象映射器。
     */
    private final ObjectMapper objectMapper;

    /**
     * AgentRun 仓储。
     */
    private final AgentRunRepository agentRunRepository;

    /**
     * EvalRun 仓储。
     */
    private final EvalRunRepository evalRunRepository;

    /**
     * EvalCaseResult 仓储。
     */
    private final EvalCaseResultRepository evalCaseResultRepository;

    /**
     * 构造 EvalRun 生命周期服务。
     *
     * @param objectMapper JSON 对象映射器
     * @param agentRunRepository AgentRun 仓储
     * @param evalRunRepository EvalRun 仓储
     * @param evalCaseResultRepository EvalCaseResult 仓储
     */
    public EvalRunLifecycleService(
            ObjectMapper objectMapper,
            AgentRunRepository agentRunRepository,
            EvalRunRepository evalRunRepository,
            EvalCaseResultRepository evalCaseResultRepository
    ) {
        this.objectMapper = objectMapper;
        this.agentRunRepository = agentRunRepository;
        this.evalRunRepository = evalRunRepository;
        this.evalCaseResultRepository = evalCaseResultRepository;
    }

    /**
     * 创建并提交 Eval 配套 AgentRun。
     *
     * @param runNo AgentRun 对外编号
     * @param agent Agent 主数据
     * @param workflowVersion 工作流版本
     * @param suite 验收套件
     * @param cases 验收用例列表
     * @return 已提交的 AgentRun
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AgentRunRecord createEvalAgentRun(
            String runNo,
            AgentRecord agent,
            WorkflowVersionRecord workflowVersion,
            EvalSuiteRecord suite,
            List<EvalCaseRecord> cases
    ) {
        AgentRunRecord inserted = agentRunRepository.insert(new AgentRunRecord(
                0L,
                runNo,
                agent.id(),
                agent.agentKey(),
                workflowVersion.id(),
                null,
                RunType.EVAL,
                buildEvalRunInput(suite, workflowVersion, cases),
                null,
                RunStatus.PENDING,
                null,
                "",
                null,
                null,
                null
        ));
        agentRunRepository.markRunning(inserted.id());
        return agentRunRepository.findById(inserted.id()).orElse(inserted);
    }

    /**
     * 创建并提交 EvalRun。
     *
     * @param runNo EvalRun 对外编号
     * @param suite 验收套件
     * @param agent Agent 主数据
     * @param workflowVersion 工作流版本
     * @param agentRun 配套 AgentRun
     * @return 已提交的 EvalRun
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EvalRunRecord createEvalRun(
            String runNo,
            EvalSuiteRecord suite,
            AgentRecord agent,
            WorkflowVersionRecord workflowVersion,
            AgentRunRecord agentRun
    ) {
        return evalRunRepository.insert(new EvalRunRecord(
                0L,
                runNo,
                suite.id(),
                agent.id(),
                workflowVersion.id(),
                suite.nodeId(),
                agentRun.id(),
                RunStatus.RUNNING,
                0,
                0,
                0,
                BigDecimal.ZERO,
                "",
                "",
                null,
                null,
                null
        ));
    }

    /**
     * 独立提交单条 EvalCaseResult。
     *
     * @param record 验收用例结果
     * @return 已提交的验收用例结果
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EvalCaseResultRecord insertEvalCaseResult(EvalCaseResultRecord record) {
        return evalCaseResultRepository.insert(record);
    }

    /**
     * 完成并提交 EvalRun 终态。
     *
     * @param evalRunId EvalRun 数据库主键
     * @param status 终态
     * @param total 总用例数
     * @param passed 通过数
     * @param failed 失败数
     * @param passRate 通过率
     * @param summary 摘要
     * @param errorMessage 错误消息
     * @param durationMs 耗时毫秒
     * @return 更新后的 EvalRun
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EvalRunRecord finishEvalRun(
            long evalRunId,
            RunStatus status,
            int total,
            int passed,
            int failed,
            BigDecimal passRate,
            String summary,
            String errorMessage,
            long durationMs
    ) {
        return evalRunRepository.finish(evalRunId, status, total, passed, failed, passRate, summary, errorMessage, durationMs);
    }

    /**
     * 完成并提交配套 AgentRun 终态。
     *
     * @param agentRunId AgentRun 数据库主键
     * @param status 终态
     * @param outputJson 输出 JSON
     * @param errorCode 错误码
     * @param errorMessage 错误消息
     * @param durationMs 耗时毫秒
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finishEvalAgentRun(
            long agentRunId,
            RunStatus status,
            JsonNode outputJson,
            String errorCode,
            String errorMessage,
            long durationMs
    ) {
        agentRunRepository.finishRun(agentRunId, status, outputJson, errorCode, errorMessage, durationMs);
    }

    /**
     * 构造 Eval 配套 AgentRun 输入。
     *
     * @param suite 验收套件
     * @param workflowVersion 工作流版本
     * @param cases 验收用例列表
     * @return AgentRun 输入 JSON
     */
    private ObjectNode buildEvalRunInput(
            EvalSuiteRecord suite,
            WorkflowVersionRecord workflowVersion,
            List<EvalCaseRecord> cases
    ) {
        ObjectNode input = objectMapper.createObjectNode();
        input.put("suiteId", suite.id());
        input.put("workflowVersionId", workflowVersion.id());
        input.put("nodeId", suite.nodeId());
        ArrayNode caseIds = input.putArray("caseIds");
        cases.forEach(evalCase -> caseIds.add(evalCase.id()));
        return input;
    }
}
