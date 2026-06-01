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
 * EvalCase MyBatis Mapper。
 */
@Mapper
public interface EvalCaseMapper {

    /**
     * 新增验收用例。
     *
     * @param record 用例记录
     * @return 新增后的用例
     */
    @ConstructorArgs({
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "suite_id", javaType = long.class),
            @Arg(column = "case_no", javaType = String.class),
            @Arg(column = "title", javaType = String.class),
            @Arg(column = "input_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "reference_answer_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "assertions_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "score_rule_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "critical", javaType = boolean.class),
            @Arg(column = "confirm_status", javaType = EvalCaseConfirmStatus.class),
            @Arg(column = "source_agent_run_id", javaType = Long.class),
            @Arg(column = "source_node_run_id", javaType = Long.class),
            @Arg(column = "source_workflow_version_id", javaType = Long.class),
            @Arg(column = "source_node_id", javaType = String.class),
            @Arg(column = "description", javaType = String.class),
            @Arg(column = "created_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "updated_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class)
    })
    @Select("""
            insert into eval_case(
              suite_id, case_no, title, input_json, reference_answer_json, assertions_json,
              score_rule_json, critical, confirm_status, source_agent_run_id, source_node_run_id,
              source_workflow_version_id, source_node_id, description
            )
            values (
              #{record.suiteId},
              #{record.caseNo},
              #{record.title},
              #{record.inputJson, typeHandler=com.myagent.common.repository.JsonNodeTypeHandler},
              #{record.referenceAnswerJson, typeHandler=com.myagent.common.repository.JsonNodeTypeHandler},
              #{record.assertionsJson, typeHandler=com.myagent.common.repository.JsonNodeTypeHandler},
              #{record.scoreRuleJson, typeHandler=com.myagent.common.repository.JsonNodeTypeHandler},
              #{record.critical},
              #{record.confirmStatus},
              #{record.sourceAgentRunId},
              #{record.sourceNodeRunId},
              #{record.sourceWorkflowVersionId},
              #{record.sourceNodeId},
              #{record.description}
            )
            returning id, suite_id, case_no, title, input_json, reference_answer_json, assertions_json,
                      score_rule_json, critical, confirm_status, source_agent_run_id, source_node_run_id,
                      source_workflow_version_id, source_node_id, description, created_at, updated_at
            """)
    EvalCaseRecord insert(@Param("record") EvalCaseRecord record);

    /**
     * 按主键查询。
     *
     * @param caseId 用例主键
     * @return 用例记录
     */
    @ConstructorArgs({
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "suite_id", javaType = long.class),
            @Arg(column = "case_no", javaType = String.class),
            @Arg(column = "title", javaType = String.class),
            @Arg(column = "input_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "reference_answer_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "assertions_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "score_rule_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "critical", javaType = boolean.class),
            @Arg(column = "confirm_status", javaType = EvalCaseConfirmStatus.class),
            @Arg(column = "source_agent_run_id", javaType = Long.class),
            @Arg(column = "source_node_run_id", javaType = Long.class),
            @Arg(column = "source_workflow_version_id", javaType = Long.class),
            @Arg(column = "source_node_id", javaType = String.class),
            @Arg(column = "description", javaType = String.class),
            @Arg(column = "created_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "updated_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class)
    })
    @Select("""
            select id, suite_id, case_no, title, input_json, reference_answer_json, assertions_json,
                   score_rule_json, critical, confirm_status, source_agent_run_id, source_node_run_id,
                   source_workflow_version_id, source_node_id, description, created_at, updated_at
            from eval_case
            where id = #{caseId}
            """)
    EvalCaseRecord findById(@Param("caseId") long caseId);

    /**
     * 分页查询用例。
     *
     * @param suiteId 套件主键
     * @param confirmStatus 确认状态
     * @param critical 是否关键用例
     * @param keyword 关键词
     * @param limit 限制条数
     * @param offset 偏移量
     * @return 用例列表
     */
    @ConstructorArgs({
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "suite_id", javaType = long.class),
            @Arg(column = "case_no", javaType = String.class),
            @Arg(column = "title", javaType = String.class),
            @Arg(column = "input_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "reference_answer_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "assertions_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "score_rule_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "critical", javaType = boolean.class),
            @Arg(column = "confirm_status", javaType = EvalCaseConfirmStatus.class),
            @Arg(column = "source_agent_run_id", javaType = Long.class),
            @Arg(column = "source_node_run_id", javaType = Long.class),
            @Arg(column = "source_workflow_version_id", javaType = Long.class),
            @Arg(column = "source_node_id", javaType = String.class),
            @Arg(column = "description", javaType = String.class),
            @Arg(column = "created_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "updated_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class)
    })
    @Select({
            "<script>",
            "select id, suite_id, case_no, title, input_json, reference_answer_json, assertions_json,",
            "       score_rule_json, critical, confirm_status, source_agent_run_id, source_node_run_id,",
            "       source_workflow_version_id, source_node_id, description, created_at, updated_at",
            "from eval_case",
            "where suite_id = #{suiteId}",
            "  <if test='confirmStatus != null'> and confirm_status = #{confirmStatus} </if>",
            "  <if test='critical != null'> and critical = #{critical} </if>",
            "  <if test='keyword != null and keyword != \"\"'>",
            "    and (lower(case_no) like concat('%', lower(#{keyword}), '%')",
            "         or lower(title) like concat('%', lower(#{keyword}), '%')",
            "         or lower(description) like concat('%', lower(#{keyword}), '%'))",
            "  </if>",
            "order by created_at desc, id desc",
            "limit #{limit} offset #{offset}",
            "</script>"
    })
    List<EvalCaseRecord> list(
            @Param("suiteId") long suiteId,
            @Param("confirmStatus") EvalCaseConfirmStatus confirmStatus,
            @Param("critical") Boolean critical,
            @Param("keyword") String keyword,
            @Param("limit") long limit,
            @Param("offset") long offset
    );

    /**
     * 统计用例数量。
     *
     * @param suiteId 套件主键
     * @param confirmStatus 确认状态
     * @param critical 是否关键用例
     * @param keyword 关键词
     * @return 总数
     */
    @Select({
            "<script>",
            "select count(*) from eval_case",
            "where suite_id = #{suiteId}",
            "  <if test='confirmStatus != null'> and confirm_status = #{confirmStatus} </if>",
            "  <if test='critical != null'> and critical = #{critical} </if>",
            "  <if test='keyword != null and keyword != \"\"'>",
            "    and (lower(case_no) like concat('%', lower(#{keyword}), '%')",
            "         or lower(title) like concat('%', lower(#{keyword}), '%')",
            "         or lower(description) like concat('%', lower(#{keyword}), '%'))",
            "  </if>",
            "</script>"
    })
    long count(
            @Param("suiteId") long suiteId,
            @Param("confirmStatus") EvalCaseConfirmStatus confirmStatus,
            @Param("critical") Boolean critical,
            @Param("keyword") String keyword
    );

    /**
     * 查询可执行用例。
     *
     * @param suiteId 套件主键
     * @param caseIds 指定用例主键
     * @return 用例列表
     */
    @ConstructorArgs({
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "suite_id", javaType = long.class),
            @Arg(column = "case_no", javaType = String.class),
            @Arg(column = "title", javaType = String.class),
            @Arg(column = "input_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "reference_answer_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "assertions_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "score_rule_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "critical", javaType = boolean.class),
            @Arg(column = "confirm_status", javaType = EvalCaseConfirmStatus.class),
            @Arg(column = "source_agent_run_id", javaType = Long.class),
            @Arg(column = "source_node_run_id", javaType = Long.class),
            @Arg(column = "source_workflow_version_id", javaType = Long.class),
            @Arg(column = "source_node_id", javaType = String.class),
            @Arg(column = "description", javaType = String.class),
            @Arg(column = "created_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "updated_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class)
    })
    @Select({
            "<script>",
            "select id, suite_id, case_no, title, input_json, reference_answer_json, assertions_json,",
            "       score_rule_json, critical, confirm_status, source_agent_run_id, source_node_run_id,",
            "       source_workflow_version_id, source_node_id, description, created_at, updated_at",
            "from eval_case",
            "where suite_id = #{suiteId}",
            "  and confirm_status in ('USER_CREATED', 'USER_CONFIRMED')",
            "  <if test='caseIds != null and caseIds.size() > 0'>",
            "    and id in",
            "    <foreach collection='caseIds' item='caseId' open='(' separator=',' close=')'>",
            "      #{caseId}",
            "    </foreach>",
            "  </if>",
            "order by id asc",
            "</script>"
    })
    List<EvalCaseRecord> listRunnableCases(
            @Param("suiteId") long suiteId,
            @Param("caseIds") List<Long> caseIds
    );

    /**
     * 统计正式用例数。
     *
     * @param suiteId 套件主键
     * @return 正式用例数
     */
    @Select("""
            select count(*)
            from eval_case
            where suite_id = #{suiteId}
              and confirm_status in ('USER_CREATED', 'USER_CONFIRMED')
            """)
    long countFormalCases(@Param("suiteId") long suiteId);

    /**
     * 更新用例内容。
     *
     * @param record 用例记录
     * @return 更新后的用例
     */
    @ConstructorArgs({
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "suite_id", javaType = long.class),
            @Arg(column = "case_no", javaType = String.class),
            @Arg(column = "title", javaType = String.class),
            @Arg(column = "input_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "reference_answer_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "assertions_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "score_rule_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "critical", javaType = boolean.class),
            @Arg(column = "confirm_status", javaType = EvalCaseConfirmStatus.class),
            @Arg(column = "source_agent_run_id", javaType = Long.class),
            @Arg(column = "source_node_run_id", javaType = Long.class),
            @Arg(column = "source_workflow_version_id", javaType = Long.class),
            @Arg(column = "source_node_id", javaType = String.class),
            @Arg(column = "description", javaType = String.class),
            @Arg(column = "created_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "updated_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class)
    })
    @Select("""
            update eval_case
            set title = #{record.title},
                input_json = #{record.inputJson, typeHandler=com.myagent.common.repository.JsonNodeTypeHandler},
                reference_answer_json = #{record.referenceAnswerJson, typeHandler=com.myagent.common.repository.JsonNodeTypeHandler},
                assertions_json = #{record.assertionsJson, typeHandler=com.myagent.common.repository.JsonNodeTypeHandler},
                score_rule_json = #{record.scoreRuleJson, typeHandler=com.myagent.common.repository.JsonNodeTypeHandler},
                critical = #{record.critical},
                description = #{record.description},
                updated_at = now()
            where id = #{record.id} and suite_id = #{record.suiteId}
            returning id, suite_id, case_no, title, input_json, reference_answer_json, assertions_json,
                      score_rule_json, critical, confirm_status, source_agent_run_id, source_node_run_id,
                      source_workflow_version_id, source_node_id, description, created_at, updated_at
            """)
    EvalCaseRecord update(@Param("record") EvalCaseRecord record);

    /**
     * 更新确认状态。
     *
     * @param suiteId 套件主键
     * @param caseId 用例主键
     * @param status 目标状态
     * @return 更新后的用例
     */
    @ConstructorArgs({
            @Arg(column = "id", javaType = long.class),
            @Arg(column = "suite_id", javaType = long.class),
            @Arg(column = "case_no", javaType = String.class),
            @Arg(column = "title", javaType = String.class),
            @Arg(column = "input_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "reference_answer_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "assertions_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "score_rule_json", javaType = JsonNode.class, typeHandler = JsonNodeTypeHandler.class),
            @Arg(column = "critical", javaType = boolean.class),
            @Arg(column = "confirm_status", javaType = EvalCaseConfirmStatus.class),
            @Arg(column = "source_agent_run_id", javaType = Long.class),
            @Arg(column = "source_node_run_id", javaType = Long.class),
            @Arg(column = "source_workflow_version_id", javaType = Long.class),
            @Arg(column = "source_node_id", javaType = String.class),
            @Arg(column = "description", javaType = String.class),
            @Arg(column = "created_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class),
            @Arg(column = "updated_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class)
    })
    @Select("""
            update eval_case
            set confirm_status = #{status},
                updated_at = now()
            where id = #{caseId} and suite_id = #{suiteId}
            returning id, suite_id, case_no, title, input_json, reference_answer_json, assertions_json,
                      score_rule_json, critical, confirm_status, source_agent_run_id, source_node_run_id,
                      source_workflow_version_id, source_node_id, description, created_at, updated_at
            """)
    EvalCaseRecord updateConfirmStatus(
            @Param("suiteId") long suiteId,
            @Param("caseId") long caseId,
            @Param("status") EvalCaseConfirmStatus status
    );
}
