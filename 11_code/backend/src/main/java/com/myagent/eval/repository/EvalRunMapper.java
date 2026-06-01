package com.myagent.eval.repository;

import com.myagent.common.repository.InstantTypeHandler;
import com.myagent.run.domain.RunStatus;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

/**
 * EvalRun MyBatis Mapper。
 */
@Mapper
public interface EvalRunMapper {

    /**
     * 新增验收运行。
     *
     * @param record 验收运行记录
     * @return 新增后的记录
     */
    @ConstructorArgs({
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "run_no", javaType = String.class),
            @Arg(column = "suite_id", javaType = long.class),
            @Arg(column = "agent_id", javaType = long.class),
            @Arg(column = "workflow_version_id", javaType = long.class),
            @Arg(column = "node_id", javaType = String.class),
            @Arg(column = "agent_run_id", javaType = long.class),
            @Arg(column = "status", javaType = RunStatus.class),
            @Arg(column = "total_case_count", javaType = int.class),
            @Arg(column = "passed_case_count", javaType = int.class),
            @Arg(column = "failed_case_count", javaType = int.class),
            @Arg(column = "pass_rate", javaType = BigDecimal.class),
            @Arg(column = "summary", javaType = String.class),
            @Arg(column = "error_message", javaType = String.class),
            @Arg(column = "started_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "finished_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "duration_ms", javaType = Long.class)
    })
    @Select("""
            insert into eval_run(
              run_no, suite_id, agent_id, workflow_version_id, node_id, agent_run_id, status,
              total_case_count, passed_case_count, failed_case_count, pass_rate, summary, error_message
            )
            values (
              #{record.runNo},
              #{record.suiteId},
              #{record.agentId},
              #{record.workflowVersionId},
              #{record.nodeId},
              #{record.agentRunId},
              #{record.status},
              #{record.totalCaseCount},
              #{record.passedCaseCount},
              #{record.failedCaseCount},
              #{record.passRate},
              #{record.summary},
              #{record.errorMessage}
            )
            returning id, run_no, suite_id, agent_id, workflow_version_id, node_id, agent_run_id, status,
                      total_case_count, passed_case_count, failed_case_count, pass_rate, summary,
                      error_message, started_at, finished_at, duration_ms
            """)
    EvalRunRecord insert(@Param("record") EvalRunRecord record);

    /**
     * 按对外编号查询。
     *
     * @param runNo 对外验收运行编号
     * @return 验收运行
     */
    @ConstructorArgs({
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "run_no", javaType = String.class),
            @Arg(column = "suite_id", javaType = long.class),
            @Arg(column = "agent_id", javaType = long.class),
            @Arg(column = "workflow_version_id", javaType = long.class),
            @Arg(column = "node_id", javaType = String.class),
            @Arg(column = "agent_run_id", javaType = long.class),
            @Arg(column = "status", javaType = RunStatus.class),
            @Arg(column = "total_case_count", javaType = int.class),
            @Arg(column = "passed_case_count", javaType = int.class),
            @Arg(column = "failed_case_count", javaType = int.class),
            @Arg(column = "pass_rate", javaType = BigDecimal.class),
            @Arg(column = "summary", javaType = String.class),
            @Arg(column = "error_message", javaType = String.class),
            @Arg(column = "started_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "finished_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "duration_ms", javaType = Long.class)
    })
    @Select("""
            select id, run_no, suite_id, agent_id, workflow_version_id, node_id, agent_run_id, status,
                   total_case_count, passed_case_count, failed_case_count, pass_rate, summary,
                   error_message, started_at, finished_at, duration_ms
            from eval_run
            where run_no = #{runNo}
            """)
    EvalRunRecord findByRunNo(@Param("runNo") String runNo);

    /**
     * 按数据库主键查询。
     *
     * @param evalRunId EvalRun 主键
     * @return 验收运行
     */
    @ConstructorArgs({
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "run_no", javaType = String.class),
            @Arg(column = "suite_id", javaType = long.class),
            @Arg(column = "agent_id", javaType = long.class),
            @Arg(column = "workflow_version_id", javaType = long.class),
            @Arg(column = "node_id", javaType = String.class),
            @Arg(column = "agent_run_id", javaType = long.class),
            @Arg(column = "status", javaType = RunStatus.class),
            @Arg(column = "total_case_count", javaType = int.class),
            @Arg(column = "passed_case_count", javaType = int.class),
            @Arg(column = "failed_case_count", javaType = int.class),
            @Arg(column = "pass_rate", javaType = BigDecimal.class),
            @Arg(column = "summary", javaType = String.class),
            @Arg(column = "error_message", javaType = String.class),
            @Arg(column = "started_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "finished_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "duration_ms", javaType = Long.class)
    })
    @Select("""
            select id, run_no, suite_id, agent_id, workflow_version_id, node_id, agent_run_id, status,
                   total_case_count, passed_case_count, failed_case_count, pass_rate, summary,
                   error_message, started_at, finished_at, duration_ms
            from eval_run
            where id = #{evalRunId}
            """)
    EvalRunRecord findById(@Param("evalRunId") long evalRunId);

    /**
     * 按关联 AgentRun 查询。
     *
     * @param agentRunId AgentRun 主键
     * @return 验收运行
     */
    @ConstructorArgs({
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "run_no", javaType = String.class),
            @Arg(column = "suite_id", javaType = long.class),
            @Arg(column = "agent_id", javaType = long.class),
            @Arg(column = "workflow_version_id", javaType = long.class),
            @Arg(column = "node_id", javaType = String.class),
            @Arg(column = "agent_run_id", javaType = long.class),
            @Arg(column = "status", javaType = RunStatus.class),
            @Arg(column = "total_case_count", javaType = int.class),
            @Arg(column = "passed_case_count", javaType = int.class),
            @Arg(column = "failed_case_count", javaType = int.class),
            @Arg(column = "pass_rate", javaType = BigDecimal.class),
            @Arg(column = "summary", javaType = String.class),
            @Arg(column = "error_message", javaType = String.class),
            @Arg(column = "started_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "finished_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "duration_ms", javaType = Long.class)
    })
    @Select("""
            select id, run_no, suite_id, agent_id, workflow_version_id, node_id, agent_run_id, status,
                   total_case_count, passed_case_count, failed_case_count, pass_rate, summary,
                   error_message, started_at, finished_at, duration_ms
            from eval_run
            where agent_run_id = #{agentRunId}
            """)
    EvalRunRecord findByAgentRunId(@Param("agentRunId") long agentRunId);

    /**
     * 查询套件运行列表。
     *
     * @param suiteId 套件主键
     * @param status 状态
     * @param startedAtFrom 开始时间下界
     * @param startedAtTo 开始时间上界
     * @param limit 限制条数
     * @param offset 偏移量
     * @return 运行列表
     */
    @ConstructorArgs({
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "run_no", javaType = String.class),
            @Arg(column = "suite_id", javaType = long.class),
            @Arg(column = "agent_id", javaType = long.class),
            @Arg(column = "workflow_version_id", javaType = long.class),
            @Arg(column = "node_id", javaType = String.class),
            @Arg(column = "agent_run_id", javaType = long.class),
            @Arg(column = "status", javaType = RunStatus.class),
            @Arg(column = "total_case_count", javaType = int.class),
            @Arg(column = "passed_case_count", javaType = int.class),
            @Arg(column = "failed_case_count", javaType = int.class),
            @Arg(column = "pass_rate", javaType = BigDecimal.class),
            @Arg(column = "summary", javaType = String.class),
            @Arg(column = "error_message", javaType = String.class),
            @Arg(column = "started_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "finished_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "duration_ms", javaType = Long.class)
    })
    @Select({
            "<script>",
            "select id, run_no, suite_id, agent_id, workflow_version_id, node_id, agent_run_id, status,",
            "       total_case_count, passed_case_count, failed_case_count, pass_rate, summary,",
            "       error_message, started_at, finished_at, duration_ms",
            "from eval_run",
            "where suite_id = #{suiteId}",
            "  <if test='status != null'> and status = #{status} </if>",
            "  <if test='startedAtFrom != null'> and started_at &gt;= #{startedAtFrom} </if>",
            "  <if test='startedAtTo != null'> and started_at &lt;= #{startedAtTo} </if>",
            "order by started_at desc, id desc",
            "limit #{limit} offset #{offset}",
            "</script>"
    })
    List<EvalRunRecord> listBySuite(
            @Param("suiteId") long suiteId,
            @Param("status") RunStatus status,
            @Param("startedAtFrom") java.time.Instant startedAtFrom,
            @Param("startedAtTo") java.time.Instant startedAtTo,
            @Param("limit") long limit,
            @Param("offset") long offset
    );

    /**
     * 统计套件运行数量。
     *
     * @param suiteId 套件主键
     * @param status 状态
     * @param startedAtFrom 开始时间下界
     * @param startedAtTo 开始时间上界
     * @return 总数
     */
    @Select({
            "<script>",
            "select count(*) from eval_run",
            "where suite_id = #{suiteId}",
            "  <if test='status != null'> and status = #{status} </if>",
            "  <if test='startedAtFrom != null'> and started_at &gt;= #{startedAtFrom} </if>",
            "  <if test='startedAtTo != null'> and started_at &lt;= #{startedAtTo} </if>",
            "</script>"
    })
    long countBySuite(
            @Param("suiteId") long suiteId,
            @Param("status") RunStatus status,
            @Param("startedAtFrom") java.time.Instant startedAtFrom,
            @Param("startedAtTo") java.time.Instant startedAtTo
    );

    /**
     * 查询历史运行。
     *
     * @param suiteId 套件主键
     * @param limit 限制条数
     * @param offset 偏移量
     * @return 历史运行列表
     */
    @ConstructorArgs({
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "run_no", javaType = String.class),
            @Arg(column = "suite_id", javaType = long.class),
            @Arg(column = "agent_id", javaType = long.class),
            @Arg(column = "workflow_version_id", javaType = long.class),
            @Arg(column = "node_id", javaType = String.class),
            @Arg(column = "agent_run_id", javaType = long.class),
            @Arg(column = "status", javaType = RunStatus.class),
            @Arg(column = "total_case_count", javaType = int.class),
            @Arg(column = "passed_case_count", javaType = int.class),
            @Arg(column = "failed_case_count", javaType = int.class),
            @Arg(column = "pass_rate", javaType = BigDecimal.class),
            @Arg(column = "summary", javaType = String.class),
            @Arg(column = "error_message", javaType = String.class),
            @Arg(column = "started_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "finished_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "duration_ms", javaType = Long.class)
    })
    @Select("""
            select id, run_no, suite_id, agent_id, workflow_version_id, node_id, agent_run_id, status,
                   total_case_count, passed_case_count, failed_case_count, pass_rate, summary,
                   error_message, started_at, finished_at, duration_ms
            from eval_run
            where suite_id = #{suiteId}
            order by started_at desc, id desc
            limit #{limit} offset #{offset}
            """)
    List<EvalRunRecord> listHistory(@Param("suiteId") long suiteId, @Param("limit") long limit, @Param("offset") long offset);

    /**
     * 统计历史运行数量。
     *
     * @param suiteId 套件主键
     * @return 总数
     */
    @Select("select count(*) from eval_run where suite_id = #{suiteId}")
    long countHistory(@Param("suiteId") long suiteId);

    /**
     * 查询上一轮验收运行。
     *
     * @param suiteId 套件主键
     * @param evalRunDbId 当前 EvalRun 主键
     * @return 上一轮运行
     */
    @ConstructorArgs({
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "run_no", javaType = String.class),
            @Arg(column = "suite_id", javaType = long.class),
            @Arg(column = "agent_id", javaType = long.class),
            @Arg(column = "workflow_version_id", javaType = long.class),
            @Arg(column = "node_id", javaType = String.class),
            @Arg(column = "agent_run_id", javaType = long.class),
            @Arg(column = "status", javaType = RunStatus.class),
            @Arg(column = "total_case_count", javaType = int.class),
            @Arg(column = "passed_case_count", javaType = int.class),
            @Arg(column = "failed_case_count", javaType = int.class),
            @Arg(column = "pass_rate", javaType = BigDecimal.class),
            @Arg(column = "summary", javaType = String.class),
            @Arg(column = "error_message", javaType = String.class),
            @Arg(column = "started_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "finished_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "duration_ms", javaType = Long.class)
    })
    @Select("""
            select id, run_no, suite_id, agent_id, workflow_version_id, node_id, agent_run_id, status,
                   total_case_count, passed_case_count, failed_case_count, pass_rate, summary,
                   error_message, started_at, finished_at, duration_ms
            from eval_run
            where suite_id = #{suiteId}
              and started_at < (select started_at from eval_run where id = #{evalRunDbId})
            order by started_at desc, id desc
            limit 1
            """)
    EvalRunRecord findPrevious(@Param("suiteId") long suiteId, @Param("evalRunDbId") long evalRunDbId);

    /**
     * 完成验收运行。
     *
     * @param evalRunId EvalRun 主键
     * @param status 状态
     * @param total 总数
     * @param passed 通过数
     * @param failed 失败数
     * @param passRate 通过率
     * @param summary 摘要
     * @param errorMessage 错误消息
     * @param durationMs 耗时毫秒
     * @return 更新后的运行
     */
    @ConstructorArgs({
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "run_no", javaType = String.class),
            @Arg(column = "suite_id", javaType = long.class),
            @Arg(column = "agent_id", javaType = long.class),
            @Arg(column = "workflow_version_id", javaType = long.class),
            @Arg(column = "node_id", javaType = String.class),
            @Arg(column = "agent_run_id", javaType = long.class),
            @Arg(column = "status", javaType = RunStatus.class),
            @Arg(column = "total_case_count", javaType = int.class),
            @Arg(column = "passed_case_count", javaType = int.class),
            @Arg(column = "failed_case_count", javaType = int.class),
            @Arg(column = "pass_rate", javaType = BigDecimal.class),
            @Arg(column = "summary", javaType = String.class),
            @Arg(column = "error_message", javaType = String.class),
            @Arg(column = "started_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "finished_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "duration_ms", javaType = Long.class)
    })
    @Select("""
            update eval_run
            set status = #{status},
                total_case_count = #{total},
                passed_case_count = #{passed},
                failed_case_count = #{failed},
                pass_rate = #{passRate},
                summary = #{summary},
                error_message = #{errorMessage},
                finished_at = now(),
                duration_ms = #{durationMs}
            where id = #{evalRunId}
            returning id, run_no, suite_id, agent_id, workflow_version_id, node_id, agent_run_id, status,
                      total_case_count, passed_case_count, failed_case_count, pass_rate, summary,
                      error_message, started_at, finished_at, duration_ms
            """)
    EvalRunRecord finish(
            @Param("evalRunId") long evalRunId,
            @Param("status") RunStatus status,
            @Param("total") int total,
            @Param("passed") int passed,
            @Param("failed") int failed,
            @Param("passRate") BigDecimal passRate,
            @Param("summary") String summary,
            @Param("errorMessage") String errorMessage,
            @Param("durationMs") long durationMs
    );
}
