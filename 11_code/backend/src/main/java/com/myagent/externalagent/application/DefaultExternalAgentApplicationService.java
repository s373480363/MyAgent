package com.myagent.externalagent.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.myagent.common.domain.EnableStatus;
import com.myagent.common.error.BizException;
import com.myagent.common.error.ErrorCode;
import com.myagent.common.page.PageResult;
import com.myagent.externalagent.application.command.ChangeExternalAgentStatusCommand;
import com.myagent.externalagent.application.command.CreateExternalAgentCommand;
import com.myagent.externalagent.application.command.TestExternalAgentCommand;
import com.myagent.externalagent.application.command.UpdateExternalAgentCommand;
import com.myagent.externalagent.application.command.UpdateExternalAgentSecretsCommand;
import com.myagent.externalagent.application.query.GetExternalAgentQuery;
import com.myagent.externalagent.application.query.ListExternalAgentsQuery;
import com.myagent.externalagent.application.result.ExternalAgentDetailResult;
import com.myagent.externalagent.application.result.ExternalAgentListItemResult;
import com.myagent.externalagent.application.result.ExternalAgentTestResult;
import com.myagent.externalagent.domain.ExternalAgentType;
import com.myagent.externalagent.repository.ExternalAgentRecord;
import com.myagent.externalagent.repository.ExternalAgentRepository;
import com.myagent.schema.repository.SchemaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 外部 Agent 应用服务默认实现。
 */
@Service
public class DefaultExternalAgentApplicationService implements ExternalAgentApplicationService {

    /**
     * 外部 Agent 仓储。
     */
    private final ExternalAgentRepository externalAgentRepository;

    /**
     * Schema 仓储。
     */
    private final SchemaRepository schemaRepository;

    /**
     * commandJson 编解码工具。
     */
    private final ExternalAgentCommandJsonCodec commandJsonCodec;

    /**
     * 测试执行器。
     */
    private final ExternalAgentTestExecutor externalAgentTestExecutor;

    /**
     * 构造外部 Agent 应用服务。
     *
     * @param externalAgentRepository 外部 Agent 仓储
     * @param schemaRepository Schema 仓储
     * @param commandJsonCodec commandJson 编解码工具
     * @param externalAgentTestExecutor 测试执行器
     */
    public DefaultExternalAgentApplicationService(
            ExternalAgentRepository externalAgentRepository,
            SchemaRepository schemaRepository,
            ExternalAgentCommandJsonCodec commandJsonCodec,
            ExternalAgentTestExecutor externalAgentTestExecutor
    ) {
        this.externalAgentRepository = externalAgentRepository;
        this.schemaRepository = schemaRepository;
        this.commandJsonCodec = commandJsonCodec;
        this.externalAgentTestExecutor = externalAgentTestExecutor;
    }

    /**
     * 分页查询外部 Agent。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    @Override
    public PageResult<ExternalAgentListItemResult> listExternalAgents(ListExternalAgentsQuery query) {
        return externalAgentRepository.listExternalAgents(query).map(this::toListItem);
    }

    /**
     * 查询外部 Agent 详情。
     *
     * @param query 查询条件
     * @return 详情结果
     */
    @Override
    public ExternalAgentDetailResult getExternalAgent(GetExternalAgentQuery query) {
        return toDetail(getExistingExternalAgent(query.adapterId()));
    }

    /**
     * 创建外部 Agent。
     *
     * @param command 创建命令
     * @return 创建后的详情
     */
    @Override
    @Transactional
    public ExternalAgentDetailResult createExternalAgent(CreateExternalAgentCommand command) {
        if (command.adapterType() != ExternalAgentType.CUSTOM_CLI && command.adapterType() != ExternalAgentType.CUSTOM_HTTP) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "平台内只允许新增 CUSTOM_CLI 和 CUSTOM_HTTP 外部 Agent。");
        }
        if (externalAgentRepository.findByAdapterKey(command.adapterKey()).isPresent()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "adapterKey 已存在，不能重复创建。");
        }
        validateOutputSchema(command.outputSchemaId());
        JsonNode normalizedCommandJson = commandJsonCodec.normalizeForCreate(
                command.adapterType(),
                command.commandJson(),
                command.secretHeaders()
        );
        ExternalAgentRecord inserted = externalAgentRepository.insert(new ExternalAgentRecord(
                0L,
                command.adapterKey().trim(),
                command.adapterType(),
                requireNonBlank(command.name(), "名称"),
                normalizedText(command.description()),
                normalizedCommandJson,
                normalizedText(command.workingDirectory()),
                normalizedTimeout(command.timeoutSeconds()),
                normalizedBoolean(command.captureStdout()),
                normalizedBoolean(command.captureStderr()),
                normalizedBoolean(command.captureGitDiff()),
                command.outputSchemaId(),
                EnableStatus.ENABLED,
                null,
                null
        ));
        return toDetail(inserted);
    }

    /**
     * 更新外部 Agent。
     *
     * @param command 更新命令
     * @return 更新后的详情
     */
    @Override
    @Transactional
    public ExternalAgentDetailResult updateExternalAgent(UpdateExternalAgentCommand command) {
        ExternalAgentRecord existing = getExistingExternalAgent(command.adapterId());
        validateOutputSchema(command.outputSchemaId());
        JsonNode normalizedCommandJson = commandJsonCodec.normalizeForUpdate(
                existing.adapterType(),
                command.commandJson(),
                command.secretHeaders(),
                existing.commandJson()
        );
        externalAgentRepository.update(new ExternalAgentRecord(
                existing.id(),
                existing.adapterKey(),
                existing.adapterType(),
                requireNonBlank(command.name(), "名称"),
                normalizedText(command.description()),
                normalizedCommandJson,
                normalizedText(command.workingDirectory()),
                normalizedTimeout(command.timeoutSeconds()),
                normalizedBoolean(command.captureStdout()),
                normalizedBoolean(command.captureStderr()),
                normalizedBoolean(command.captureGitDiff()),
                command.outputSchemaId(),
                existing.status(),
                existing.createdAt(),
                existing.updatedAt()
        ));
        return toDetail(getExistingExternalAgent(command.adapterId()));
    }

    /**
     * 单独更新敏感 secret。
     *
     * @param command 更新命令
     * @return 更新后的详情
     */
    @Override
    @Transactional
    public ExternalAgentDetailResult updateExternalAgentSecrets(UpdateExternalAgentSecretsCommand command) {
        ExternalAgentRecord existing = getExistingExternalAgent(command.adapterId());
        if (existing.adapterType() != ExternalAgentType.CUSTOM_HTTP) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "只有 CUSTOM_HTTP 支持敏感 header secret 更新。");
        }
        JsonNode updatedCommandJson = commandJsonCodec.updateSecrets(
                existing.commandJson(),
                command.items(),
                command.clearHeaderNames()
        );
        externalAgentRepository.update(new ExternalAgentRecord(
                existing.id(),
                existing.adapterKey(),
                existing.adapterType(),
                existing.name(),
                existing.description(),
                updatedCommandJson,
                existing.workingDirectory(),
                existing.timeoutSeconds(),
                existing.captureStdout(),
                existing.captureStderr(),
                existing.captureGitDiff(),
                existing.outputSchemaId(),
                existing.status(),
                existing.createdAt(),
                existing.updatedAt()
        ));
        return toDetail(getExistingExternalAgent(command.adapterId()));
    }

    /**
     * 更新外部 Agent 状态。
     *
     * @param command 更新命令
     */
    @Override
    @Transactional
    public void changeExternalAgentStatus(ChangeExternalAgentStatusCommand command) {
        getExistingExternalAgent(command.adapterId());
        externalAgentRepository.updateStatus(command.adapterId(), command.status());
    }

    /**
     * 测试外部 Agent。
     *
     * @param command 测试命令
     * @return 测试结果
     */
    @Override
    public ExternalAgentTestResult testExternalAgent(TestExternalAgentCommand command) {
        ExternalAgentRecord existing = getExistingExternalAgent(command.adapterId());
        commandJsonCodec.assertSecretsConfigured(existing.adapterType(), existing.commandJson());
        return externalAgentTestExecutor.execute(existing, normalizedText(command.prompt()), command.input());
    }

    /**
     * 校验输出 Schema。
     *
     * @param outputSchemaId 输出 Schema 主键
     */
    private void validateOutputSchema(Long outputSchemaId) {
        if (outputSchemaId == null) {
            return;
        }
        schemaRepository.findById(outputSchemaId)
                .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND, "指定输出 Schema 不存在。"));
    }

    /**
     * 获取已存在的外部 Agent。
     *
     * @param adapterId 外部 Agent 主键
     * @return 持久化记录
     */
    private ExternalAgentRecord getExistingExternalAgent(long adapterId) {
        return externalAgentRepository.findById(adapterId)
                .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND, "指定外部 Agent 不存在。"));
    }

    /**
     * 转换列表项结果。
     *
     * @param record 持久化记录
     * @return 列表项结果
     */
    private ExternalAgentListItemResult toListItem(ExternalAgentRecord record) {
        return new ExternalAgentListItemResult(
                record.id(),
                record.adapterKey(),
                record.adapterType(),
                record.name(),
                record.description(),
                record.timeoutSeconds(),
                record.outputSchemaId(),
                record.status(),
                record.updatedAt()
        );
    }

    /**
     * 转换详情结果。
     *
     * @param record 持久化记录
     * @return 详情结果
     */
    private ExternalAgentDetailResult toDetail(ExternalAgentRecord record) {
        return new ExternalAgentDetailResult(
                record.id(),
                record.adapterKey(),
                record.adapterType(),
                record.name(),
                record.description(),
                commandJsonCodec.sanitizeForResponse(record.adapterType(), record.commandJson()),
                commandJsonCodec.listSecretHeaderMetas(record.adapterType(), record.commandJson()),
                record.workingDirectory(),
                record.timeoutSeconds(),
                record.captureStdout(),
                record.captureStderr(),
                record.captureGitDiff(),
                record.outputSchemaId(),
                record.status(),
                record.createdAt(),
                record.updatedAt()
        );
    }

    /**
     * 规范化必填文本。
     *
     * @param value 原始文本
     * @param fieldName 字段名
     * @return 规范化文本
     */
    private String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, fieldName + "不能为空。");
        }
        return value.trim();
    }

    /**
     * 规范化可选文本。
     *
     * @param value 原始文本
     * @return 规范化文本
     */
    private String normalizedText(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * 规范化超时配置。
     *
     * @param timeoutSeconds 原始超时
     * @return 规范化后的超时秒数
     */
    private int normalizedTimeout(Integer timeoutSeconds) {
        if (timeoutSeconds == null) {
            return 600;
        }
        if (timeoutSeconds <= 0) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "timeoutSeconds 必须大于 0。");
        }
        return timeoutSeconds;
    }

    /**
     * 规范化布尔值。
     *
     * @param value 原始布尔值
     * @return 规范化布尔值
     */
    private boolean normalizedBoolean(Boolean value) {
        return Boolean.TRUE.equals(value);
    }
}
