package com.myagent.eval.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.myagent.common.repository.InstantTypeHandler;
import com.myagent.common.repository.JsonNodeTypeHandler;
import com.myagent.eval.domain.EvalCaseConfirmStatus;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * EvalCaseResult MyBatis Mapper。
 */
@Mapper
public interface EvalCaseResultMapper {

    /**
     * 新增验收用例结果。
     *
     * @param record 用例结果记录
     * @return 新增后的结果
     */
    @ConstructorArgs({
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "eval_run_id", javaType = long.class),
            @Arg(column = "eval_case_id", javaType = long.class),
            @Arg(column = "output_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "hard_check_result_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "judge_result_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "judge_raw_text", javaType = String.class),
            @Arg(column = "judge_model_offering_key", javaType = String.class),
            @Arg(column = "judge_prompt_version", javaType = String.class),
            @Arg(column = "passed", javaType = boolean.class),
            @Arg(column = "error_message", javaType = String.class),
            @Arg(column = "duration_ms", javaType = Long.class),
            @Arg(column = "created_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class)
    })
    @Select("""
            insert into eval_case_result(
              eval_run_id, eval_case_id, output_json, hard_check_result_json, judge_result_json,
              judge_raw_text, judge_model_offering_key, judge_prompt_version, passed, error_message, duration_ms
            )
            values (
              #{record.evalRunId},
              #{record.evalCaseId},
              #{record.outputJson, typeHandler=com.myagent.common.repository.JsonNodeTypeHandler},
              #{record.hardCheckResultJson, typeHandler=com.myagent.common.repository.JsonNodeTypeHandler},
              #{record.judgeResultJson, typeHandler=com.myagent.common.repository.JsonNodeTypeHandler},
              #{record.judgeRawText},
              #{record.judgeModelOfferingKey},
              #{record.judgePromptVersion},
              #{record.passed},
              #{record.errorMessage},
              #{record.durationMs}
            )
            returning id, eval_run_id, eval_case_id, output_json, hard_check_result_json, judge_result_json,
                      judge_raw_text, judge_model_offering_key, judge_prompt_version, passed, error_message,
                      duration_ms, created_at
            """)
    EvalCaseResultRecord insert(@Param("record") EvalCaseResultRecord record);

    /**
     * 查询验收结果明细。
     *
     * @param evalRunId EvalRun 主键
     * @param passed 是否通过
     * @param critical 是否关键用例
     * @param keyword 关键词
     * @param limit 限制条数
     * @param offset 偏移量
     * @return 联表结果
     */
    @ConstructorArgs({
            @Arg(column = "result_id", javaType = long.class),
            @Arg(column = "eval_run_id", javaType = long.class),
            @Arg(column = "eval_case_id", javaType = long.class),
            @Arg(column = "output_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "hard_check_result_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "judge_result_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "judge_raw_text", javaType = String.class),
            @Arg(column = "judge_model_offering_key", javaType = String.class),
            @Arg(column = "judge_prompt_version", javaType = String.class),
            @Arg(column = "passed", javaType = boolean.class),
            @Arg(column = "error_message", javaType = String.class),
            @Arg(column = "duration_ms", javaType = Long.class),
            @Arg(column = "case_no", javaType = String.class),
            @Arg(column = "title", javaType = String.class),
            @Arg(column = "input_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "reference_sample_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "judge_rule_text", javaType = String.class),
            @Arg(column = "hard_checks_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "critical", javaType = boolean.class),
            @Arg(column = "confirm_status", javaType = EvalCaseConfirmStatus.class)
    })
    @Select({
            "<script>",
            "select r.id as result_id, r.eval_run_id, r.eval_case_id, r.output_json,",
            "       r.hard_check_result_json, r.judge_result_json, r.judge_raw_text,",
            "       r.judge_model_offering_key, r.judge_prompt_version, r.passed, r.error_message, r.duration_ms,",
            "       c.case_no, c.title, c.input_json, c.reference_sample_json, c.judge_rule_text, c.hard_checks_json,",
            "       c.critical, c.confirm_status",
            "from eval_case_result r",
            "join eval_case c on c.id = r.eval_case_id",
            "where r.eval_run_id = #{evalRunId}",
            "  <if test='passed != null'> and r.passed = #{passed} </if>",
            "  <if test='critical != null'> and c.critical = #{critical} </if>",
            "  <if test='keyword != null and keyword != \"\"'>",
            "    and (lower(c.case_no) like concat('%', lower(#{keyword}), '%')",
            "         or lower(c.title) like concat('%', lower(#{keyword}), '%')",
            "         or lower(r.error_message) like concat('%', lower(#{keyword}), '%'))",
            "  </if>",
            "order by c.id asc",
            "limit #{limit} offset #{offset}",
            "</script>"
    })
    List<EvalCaseResultJoinedRecord> listByEvalRun(
            @Param("evalRunId") long evalRunId,
            @Param("passed") Boolean passed,
            @Param("critical") Boolean critical,
            @Param("keyword") String keyword,
            @Param("limit") long limit,
            @Param("offset") long offset
    );

    /**
     * 统计验收结果数量。
     *
     * @param evalRunId EvalRun 主键
     * @param passed 是否通过
     * @param critical 是否关键用例
     * @param keyword 关键词
     * @return 总数
     */
    @Select({
            "<script>",
            "select count(*)",
            "from eval_case_result r",
            "join eval_case c on c.id = r.eval_case_id",
            "where r.eval_run_id = #{evalRunId}",
            "  <if test='passed != null'> and r.passed = #{passed} </if>",
            "  <if test='critical != null'> and c.critical = #{critical} </if>",
            "  <if test='keyword != null and keyword != \"\"'>",
            "    and (lower(c.case_no) like concat('%', lower(#{keyword}), '%')",
            "         or lower(c.title) like concat('%', lower(#{keyword}), '%')",
            "         or lower(r.error_message) like concat('%', lower(#{keyword}), '%'))",
            "  </if>",
            "</script>"
    })
    long countByEvalRun(
            @Param("evalRunId") long evalRunId,
            @Param("passed") Boolean passed,
            @Param("critical") Boolean critical,
            @Param("keyword") String keyword
    );

    /**
     * 查询失败明细。
     *
     * @param evalRunId EvalRun 主键
     * @return 失败列表
     */
    @ConstructorArgs({
            @Arg(column = "result_id", javaType = long.class),
            @Arg(column = "eval_run_id", javaType = long.class),
            @Arg(column = "eval_case_id", javaType = long.class),
            @Arg(column = "output_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "hard_check_result_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "judge_result_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "judge_raw_text", javaType = String.class),
            @Arg(column = "judge_model_offering_key", javaType = String.class),
            @Arg(column = "judge_prompt_version", javaType = String.class),
            @Arg(column = "passed", javaType = boolean.class),
            @Arg(column = "error_message", javaType = String.class),
            @Arg(column = "duration_ms", javaType = Long.class),
            @Arg(column = "case_no", javaType = String.class),
            @Arg(column = "title", javaType = String.class),
            @Arg(column = "input_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "reference_sample_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "judge_rule_text", javaType = String.class),
            @Arg(column = "hard_checks_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "critical", javaType = boolean.class),
            @Arg(column = "confirm_status", javaType = EvalCaseConfirmStatus.class)
    })
    @Select("""
            select r.id as result_id, r.eval_run_id, r.eval_case_id, r.output_json,
                   r.hard_check_result_json, r.judge_result_json, r.judge_raw_text,
                   r.judge_model_offering_key, r.judge_prompt_version, r.passed, r.error_message, r.duration_ms,
                   c.case_no, c.title, c.input_json, c.reference_sample_json, c.judge_rule_text, c.hard_checks_json,
                   c.critical, c.confirm_status
            from eval_case_result r
            join eval_case c on c.id = r.eval_case_id
            where r.eval_run_id = #{evalRunId}
              and r.passed = false
            order by c.critical desc, c.id asc
            """)
    List<EvalCaseResultJoinedRecord> listFailed(@Param("evalRunId") long evalRunId);

    /**
     * 统计关键失败数。
     *
     * @param evalRunId EvalRun 主键
     * @return 关键失败数
     */
    @Select("""
            select count(*)
            from eval_case_result r
            join eval_case c on c.id = r.eval_case_id
            where r.eval_run_id = #{evalRunId}
              and r.passed = false
              and c.critical = true
            """)
    long countCriticalFailures(@Param("evalRunId") long evalRunId);
}
