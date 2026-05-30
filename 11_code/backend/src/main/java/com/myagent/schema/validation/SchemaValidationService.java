package com.myagent.schema.validation;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Schema 校验服务。
 */
public interface SchemaValidationService {

    /**
     * 按 Schema 引用校验业务载荷。
     *
     * @param payload 待校验业务载荷
     * @param schemaRef Schema 引用
     * @param stage 校验阶段
     * @return 校验结果
     */
    SchemaValidationResult validate(JsonNode payload, SchemaReference schemaRef, ValidationStage stage);
}
