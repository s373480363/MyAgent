package com.myagent.runtime.executor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myagent.common.error.BizException;
import com.myagent.common.error.ErrorCode;
import com.myagent.runtime.NodeExecutionContext;
import com.myagent.schema.validation.SchemaReference;
import com.myagent.schema.validation.SchemaValidationResult;
import com.myagent.schema.validation.ValidationStage;
import com.myagent.workflow.domain.WorkflowSchemaRef;

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
     * 校验 Schema 引用。
     *
     * @param context 节点执行上下文
     * @param payload 载荷
     * @param schemaRef 工作流 Schema 引用
     * @param stage 校验阶段
     */
    protected void validateSchema(
            NodeExecutionContext context,
            JsonNode payload,
            WorkflowSchemaRef schemaRef,
            ValidationStage stage
    ) {
        if (schemaRef == null) {
            return;
        }
        SchemaValidationResult result = context.schemaValidationService().validate(payload, toSchemaReference(schemaRef), stage);
        if (!result.isValid()) {
            throw new BizException(ErrorCode.SCHEMA_VALIDATION_FAILED, "节点 Schema 校验失败。");
        }
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
}
