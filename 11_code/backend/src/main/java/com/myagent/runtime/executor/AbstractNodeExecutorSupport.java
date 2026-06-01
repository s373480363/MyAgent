package com.myagent.runtime.executor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.myagent.common.api.ApiError;
import com.myagent.common.error.BizException;
import com.myagent.common.error.ErrorCode;
import com.myagent.run.domain.TraceEventType;
import com.myagent.runtime.NodeExecutionContext;
import com.myagent.runtime.NodeOutputSchemaValidationException;
import com.myagent.runtime.SchemaValidationRecord;
import com.myagent.runtime.TraceEventRecord;
import com.myagent.schema.validation.SchemaReference;
import com.myagent.schema.validation.SchemaValidationError;
import com.myagent.schema.validation.SchemaValidationResult;
import com.myagent.schema.validation.ValidationStage;
import com.myagent.workflow.domain.WorkflowSchemaRef;

import java.math.BigDecimal;
import java.util.List;

/**
 * 节点执行器公共支持逻辑。
 */
abstract class AbstractNodeExecutorSupport {

    /**
     * JSON 对象映射器。
     */
    protected final ObjectMapper objectMapper;

    /**
     * 构造公共支持逻辑。
     *
     * @param objectMapper JSON 对象映射器
     */
    AbstractNodeExecutorSupport(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 根据节点输入映射提取输入。
     *
     * @param context 节点执行上下文
     * @return 节点输入
     */
    protected JsonNode extractInput(NodeExecutionContext context) {
        return context.mappingService().extractInput(
                context.workflowContext().root(),
                context.nodeDefinition().getInputMapping()
        );
    }

    /**
     * 校验 Schema 引用，并把结果同步写入节点上下文与 Trace。
     *
     * @param context 节点执行上下文
     * @param payload 载荷
     * @param schemaRef 工作流 Schema 引用
     * @param stage 校验阶段
     * @return Schema 校验结果；未配置 Schema 时返回 null
     */
    protected SchemaValidationResult validateSchema(
            NodeExecutionContext context,
            JsonNode payload,
            WorkflowSchemaRef schemaRef,
            ValidationStage stage
    ) {
        if (schemaRef == null) {
            return null;
        }
        SchemaValidationResult result = context.schemaValidationService().validate(payload, toSchemaReference(schemaRef), stage);
        context.schemaValidationResults().add(new SchemaValidationRecord(stage, result));
        context.traceWriter().writeEvent(new TraceEventRecord(
                context.agentRunDbId(),
                context.nodeRunDbId(),
                null,
                TraceEventType.SCHEMA_VALIDATION,
                result.isValid() ? "Schema 校验通过：" + stage.getCode() : "Schema 校验失败：" + stage.getCode(),
                schemaValidationDetail(stage, result)
        ));
        if (!result.isValid()) {
            throw new BizException(
                    ErrorCode.SCHEMA_VALIDATION_FAILED,
                    "节点 Schema 校验失败：" + firstErrorMessage(result),
                    toApiErrorDetails(result)
            );
        }
        return result;
    }

    /**
     * 校验节点输出 Schema，失败时携带已产生输出供 NodeRun 落库。
     *
     * @param context 节点执行上下文
     * @param outputJson 已产生节点输出
     * @param schemaRef 工作流 Schema 引用
     * @param stage 校验阶段
     * @return Schema 校验结果；未配置 Schema 时返回 null
     */
    protected SchemaValidationResult validateOutputSchema(
            NodeExecutionContext context,
            JsonNode outputJson,
            WorkflowSchemaRef schemaRef,
            ValidationStage stage
    ) {
        try {
            return validateSchema(context, outputJson, schemaRef, stage);
        } catch (BizException exception) {
            if (exception.getErrorCode() == ErrorCode.SCHEMA_VALIDATION_FAILED) {
                throw new NodeOutputSchemaValidationException(outputJson, exception);
            }
            throw exception;
        }
    }

    /**
     * 构造模型类节点的正式用户提示词。
     *
     * @param context 节点执行上下文
     * @param input 节点输入
     * @return 渲染后的用户提示词
     */
    protected String renderUserPromptTemplate(NodeExecutionContext context, JsonNode input) {
        JsonNode config = context.nodeDefinition().getConfig();
        String template = readText(config, "userPromptTemplate", null);
        if (template == null || template.isBlank()) {
            throw new BizException(ErrorCode.NODE_EXECUTION_FAILED, "模型类节点必须配置 userPromptTemplate。");
        }
        return renderTemplate(template, context, input);
    }

    /**
     * 构造模型类节点的系统提示词。
     *
     * @param context 节点执行上下文
     * @param input 节点输入
     * @return 渲染后的系统提示词
     */
    protected String renderSystemPromptTemplate(NodeExecutionContext context, JsonNode input) {
        JsonNode config = context.nodeDefinition().getConfig();
        String template = readText(config, "systemPromptTemplate", context.agentDefinition().systemPrompt());
        return template == null ? null : renderTemplate(template, context, input);
    }

    /**
     * 读取文本配置。
     *
     * @param config 配置节点
     * @param fieldName 字段名
     * @param fallback 回退值
     * @return 文本值
     */
    protected String readText(JsonNode config, String fieldName, String fallback) {
        if (config != null && config.hasNonNull(fieldName) && config.get(fieldName).isTextual()) {
            return config.get(fieldName).asText();
        }
        return fallback;
    }

    /**
     * 读取数字配置。
     *
     * @param config 配置节点
     * @param fieldName 字段名
     * @param fallback 回退值
     * @return 数字值
     */
    protected BigDecimal readDecimal(JsonNode config, String fieldName, BigDecimal fallback) {
        if (config != null && config.hasNonNull(fieldName) && config.get(fieldName).isNumber()) {
            return config.get(fieldName).decimalValue();
        }
        return fallback;
    }

    /**
     * 渲染受控提示词占位符。
     *
     * @param template 模板文本
     * @param context 节点执行上下文
     * @param input 节点输入
     * @return 渲染后的文本
     */
    protected String renderTemplate(String template, NodeExecutionContext context, JsonNode input) {
        return template
                .replace("{inputJson}", input == null ? "null" : input.toString())
                .replace("{agentKey}", context.agentDefinition().agentKey())
                .replace("{nodeId}", context.nodeDefinition().getNodeId());
    }

    /**
     * 转换 Schema 引用。
     *
     * @param schemaRef 工作流 Schema 引用
     * @return 运行时 Schema 引用
     */
    private SchemaReference toSchemaReference(WorkflowSchemaRef schemaRef) {
        if (schemaRef.getSchemaKey() != null && schemaRef.getVersion() != null) {
            return SchemaReference.byKeyAndVersion(schemaRef.getSchemaKey(), schemaRef.getVersion());
        }
        throw new BizException(ErrorCode.INVALID_ARGUMENT, "Schema 引用不完整。");
    }

    /**
     * 构造 Schema 校验 Trace 详情。
     *
     * @param stage 校验阶段
     * @param result 校验结果
     * @return Trace 详情
     */
    private ObjectNode schemaValidationDetail(ValidationStage stage, SchemaValidationResult result) {
        ObjectNode detail = objectMapper.createObjectNode()
                .put("stage", stage.getCode())
                .put("valid", result.isValid());
        if (result.getSchemaKey() != null) {
            detail.put("schemaKey", result.getSchemaKey());
        }
        if (result.getSchemaVersion() != null) {
            detail.put("version", result.getSchemaVersion());
        }
        ArrayNode errors = detail.putArray("errors");
        for (SchemaValidationError error : result.getErrors()) {
            errors.add(objectMapper.createObjectNode()
                    .put("path", error.getPath())
                    .put("keyword", error.getKeyword())
                    .put("message", error.getMessage()));
        }
        return detail;
    }

    /**
     * 转换为统一 API 字段级错误明细。
     *
     * @param result Schema 校验结果
     * @return 字段级错误明细
     */
    private List<ApiError.Detail> toApiErrorDetails(SchemaValidationResult result) {
        return result.getErrors().stream()
                .map(error -> ApiError.Detail.of(error.getPath(), error.getKeyword(), error.getMessage()))
                .toList();
    }

    /**
     * 读取首个 Schema 错误消息。
     *
     * @param result Schema 校验结果
     * @return 中文错误消息
     */
    private String firstErrorMessage(SchemaValidationResult result) {
        return result.getErrors().isEmpty()
                ? "字段不符合 Schema。"
                : result.getErrors().getFirst().getPath() + " " + result.getErrors().getFirst().getMessage();
    }
}
