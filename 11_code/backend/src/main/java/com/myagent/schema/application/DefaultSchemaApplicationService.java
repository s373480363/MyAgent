package com.myagent.schema.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.myagent.common.error.BizException;
import com.myagent.common.error.ErrorCode;
import com.myagent.common.page.PageResult;
import com.myagent.schema.application.command.CreateSchemaCommand;
import com.myagent.schema.application.command.CreateSchemaVersionCommand;
import com.myagent.schema.application.command.UpdateSchemaDraftCommand;
import com.myagent.schema.application.query.GetSchemaQuery;
import com.myagent.schema.application.query.ListSchemasQuery;
import com.myagent.schema.application.result.SchemaDetailResult;
import com.myagent.schema.application.result.SchemaListItemResult;
import com.myagent.schema.domain.SchemaStatus;
import com.myagent.schema.repository.SchemaRecord;
import com.myagent.schema.repository.SchemaRepository;
import com.myagent.schema.validation.SchemaDefinitionValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Schema 应用服务默认实现。
 */
@Service
public class DefaultSchemaApplicationService implements SchemaApplicationService {

    /**
     * Schema 仓储。
     */
    private final SchemaRepository schemaRepository;

    /**
     * Schema 定义保存前校验器。
     */
    private final SchemaDefinitionValidator schemaDefinitionValidator;

    /**
     * 构造 Schema 应用服务。
     *
     * @param schemaRepository Schema 仓储
     * @param schemaDefinitionValidator Schema 定义校验器
     */
    public DefaultSchemaApplicationService(
            SchemaRepository schemaRepository,
            SchemaDefinitionValidator schemaDefinitionValidator
    ) {
        this.schemaRepository = schemaRepository;
        this.schemaDefinitionValidator = schemaDefinitionValidator;
    }

    /**
     * 分页查询 Schema。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    @Override
    public PageResult<SchemaListItemResult> listSchemas(ListSchemasQuery query) {
        return schemaRepository.listSchemas(query).map(this::toListItemResult);
    }

    /**
     * 创建 Schema。
     *
     * @param command 创建命令
     * @return Schema 详情
     */
    @Override
    @Transactional
    public SchemaDetailResult createSchema(CreateSchemaCommand command) {
        validateSchemaCommand(command.name(), command.description(), command.jsonSchema(), command.javaType());
        if (command.sourceSchemaId() != null) {
            getExistingSchema(command.sourceSchemaId());
        }
        int nextVersion = schemaRepository.findMaxVersion(command.schemaKey()) + 1;
        SchemaRecord record = new SchemaRecord(
                0L,
                command.schemaKey(),
                nextVersion,
                command.name(),
                normalizedDescription(command.description()),
                command.jsonSchema(),
                normalizedJavaType(command.javaType()),
                command.createdFrom(),
                SchemaStatus.DRAFT,
                false,
                null,
                null
        );
        return toDetailResult(schemaRepository.insert(record));
    }

    /**
     * 更新 Schema 草稿。
     *
     * @param command 更新命令
     * @return Schema 详情
     */
    @Override
    @Transactional
    public SchemaDetailResult updateSchemaDraft(UpdateSchemaDraftCommand command) {
        validateSchemaCommand(command.name(), command.description(), command.jsonSchema(), command.javaType());
        SchemaRecord existing = getExistingSchema(command.schemaId());
        ensureEditableDraft(existing);
        SchemaRecord updated = new SchemaRecord(
                existing.getId(),
                existing.getSchemaKey(),
                existing.getVersion(),
                command.name(),
                normalizedDescription(command.description()),
                command.jsonSchema(),
                normalizedJavaType(command.javaType()),
                existing.getCreatedFrom(),
                existing.getStatus(),
                existing.isLocked(),
                existing.getCreatedAt(),
                existing.getUpdatedAt()
        );
        int affected = schemaRepository.updateDraft(updated);
        if (affected == 0) {
            throw new BizException(ErrorCode.SCHEMA_VALIDATION_FAILED, "Schema 草稿当前不可编辑。");
        }
        return toDetailResult(getExistingSchema(command.schemaId()));
    }

    /**
     * 查询 Schema 详情。
     *
     * @param query 查询条件
     * @return Schema 详情
     */
    @Override
    public SchemaDetailResult getSchema(GetSchemaQuery query) {
        return toDetailResult(getExistingSchema(query.schemaId()));
    }

    /**
     * 基于旧版本创建新版本。
     *
     * @param command 创建版本命令
     * @return Schema 详情
     */
    @Override
    @Transactional
    public SchemaDetailResult createSchemaVersion(CreateSchemaVersionCommand command) {
        validateSchemaCommand(command.name(), command.description(), command.jsonSchema(), command.javaType());
        SchemaRecord source = getExistingSchema(command.schemaId());
        int nextVersion = schemaRepository.findMaxVersion(source.getSchemaKey()) + 1;
        SchemaRecord record = new SchemaRecord(
                0L,
                source.getSchemaKey(),
                nextVersion,
                command.name(),
                normalizedDescription(command.description()),
                command.jsonSchema(),
                normalizedJavaType(command.javaType()),
                source.getCreatedFrom(),
                SchemaStatus.DRAFT,
                false,
                null,
                null
        );
        return toDetailResult(schemaRepository.insert(record));
    }

    /**
     * 锁定 Schema 版本。
     *
     * @param schemaId Schema 主键
     */
    @Override
    @Transactional
    public void lockSchemaVersion(long schemaId) {
        int affected = schemaRepository.lockSchemaVersion(schemaId);
        if (affected == 0) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "指定 Schema 不存在。");
        }
    }

    /**
     * 验证 Schema 保存内容。
     *
     * @param name 中文名称
     * @param description 描述
     * @param jsonSchema JSON Schema 内容
     * @param javaType Java 类型
     */
    private void validateSchemaCommand(String name, String description, JsonNode jsonSchema, String javaType) {
        if (name == null || name.isBlank()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Schema 名称不能为空。");
        }
        schemaDefinitionValidator.validateDefinition(jsonSchema);
        normalizedDescription(description);
        normalizedJavaType(javaType);
    }

    /**
     * 获取 Schema 记录。
     *
     * @param schemaId Schema 主键
     * @return Schema 记录
     */
    private SchemaRecord getExistingSchema(long schemaId) {
        return schemaRepository.findById(schemaId)
                .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND, "指定 Schema 不存在。"));
    }

    /**
     * 验证是否可编辑草稿。
     *
     * @param record Schema 记录
     */
    private void ensureEditableDraft(SchemaRecord record) {
        if (record.getStatus() != SchemaStatus.DRAFT || record.isLocked()) {
            throw new BizException(ErrorCode.SCHEMA_VALIDATION_FAILED, "仅 DRAFT 且未锁定的 Schema 可编辑。");
        }
    }

    /**
     * 规范化描述。
     *
     * @param description 描述
     * @return 规范化描述
     */
    private String normalizedDescription(String description) {
        return description == null ? "" : description;
    }

    /**
     * 规范化 Java 类型。
     *
     * @param javaType Java 类型
     * @return 规范化 Java 类型
     */
    private String normalizedJavaType(String javaType) {
        return javaType == null ? "" : javaType;
    }

    /**
     * 转换列表项。
     *
     * @param record Schema 记录
     * @return 列表项结果
     */
    private SchemaListItemResult toListItemResult(SchemaRecord record) {
        return new SchemaListItemResult(
                record.getId(),
                record.getSchemaKey(),
                record.getVersion(),
                record.getName(),
                record.getDescription(),
                record.getJsonSchema(),
                record.getJavaType(),
                record.getCreatedFrom(),
                record.getStatus(),
                record.isLocked(),
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }

    /**
     * 转换详情结果。
     *
     * @param record Schema 记录
     * @return 详情结果
     */
    private SchemaDetailResult toDetailResult(SchemaRecord record) {
        return new SchemaDetailResult(
                record.getId(),
                record.getSchemaKey(),
                record.getVersion(),
                record.getName(),
                record.getDescription(),
                record.getJsonSchema(),
                record.getJavaType(),
                record.getCreatedFrom(),
                record.getStatus(),
                record.isLocked(),
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }
}
