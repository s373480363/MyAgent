package com.myagent.modelcatalog.application;

import com.myagent.common.page.PageResult;
import com.myagent.modelcatalog.application.command.ChangeModelOfferingStatusCommand;
import com.myagent.modelcatalog.application.command.ChangeModelProviderStatusCommand;
import com.myagent.modelcatalog.application.command.CreateModelOfferingCommand;
import com.myagent.modelcatalog.application.command.CreateModelProviderCommand;
import com.myagent.modelcatalog.application.command.TestModelProviderCommand;
import com.myagent.modelcatalog.application.command.UpdateModelOfferingCommand;
import com.myagent.modelcatalog.application.command.UpdateModelProviderCommand;
import com.myagent.modelcatalog.application.command.UpdateModelProviderSecretsCommand;
import com.myagent.modelcatalog.application.query.GetModelProviderQuery;
import com.myagent.modelcatalog.application.query.ListModelOfferingsQuery;
import com.myagent.modelcatalog.application.query.ListModelProvidersQuery;
import com.myagent.modelcatalog.application.result.ModelOfferingBatchResult;
import com.myagent.modelcatalog.application.result.ModelOfferingDescriptor;
import com.myagent.modelcatalog.application.result.ModelProviderResult;
import com.myagent.modelcatalog.application.result.ModelProviderTestResult;

import java.util.List;

/**
 * 模型目录应用服务。
 */
public interface ModelCatalogApplicationService {

    /**
     * 分页查询模型供应商。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    PageResult<ModelProviderResult> listModelProviders(ListModelProvidersQuery query);

    /**
     * 查询模型供应商详情。
     *
     * @param query 查询条件
     * @return 详情
     */
    ModelProviderResult getModelProvider(GetModelProviderQuery query);

    /**
     * 创建模型供应商。
     *
     * @param command 创建命令
     * @return 详情
     */
    ModelProviderResult createModelProvider(CreateModelProviderCommand command);

    /**
     * 更新模型供应商。
     *
     * @param command 更新命令
     * @return 详情
     */
    ModelProviderResult updateModelProvider(UpdateModelProviderCommand command);

    /**
     * 更新模型供应商密钥。
     *
     * @param command 更新命令
     * @return 详情
     */
    ModelProviderResult updateModelProviderSecrets(UpdateModelProviderSecretsCommand command);

    /**
     * 修改模型供应商状态。
     *
     * @param command 命令
     */
    void changeModelProviderStatus(ChangeModelProviderStatusCommand command);

    /**
     * 测试模型供应商连接。
     *
     * @param command 命令
     * @return 测试结果
     */
    ModelProviderTestResult testModelProvider(TestModelProviderCommand command);

    /**
     * 分页查询模型供应项。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    PageResult<ModelOfferingDescriptor> listModelOfferings(ListModelOfferingsQuery query);

    /**
     * 查询模型供应项详情。
     *
     * @param offeringKey 供应项标识
     * @return 详情
     */
    ModelOfferingDescriptor getModelOffering(String offeringKey);

    /**
     * 按键批量查询模型供应项。
     *
     * @param offeringKeys 供应项键列表
     * @return 查询结果
     */
    ModelOfferingBatchResult getModelOfferingsByKeys(List<String> offeringKeys);

    /**
     * 创建模型供应项。
     *
     * @param command 创建命令
     * @return 详情
     */
    ModelOfferingDescriptor createModelOffering(CreateModelOfferingCommand command);

    /**
     * 更新模型供应项。
     *
     * @param command 更新命令
     * @return 详情
     */
    ModelOfferingDescriptor updateModelOffering(UpdateModelOfferingCommand command);

    /**
     * 修改模型供应项状态。
     *
     * @param command 命令
     * @return 最新详情
     */
    ModelOfferingDescriptor changeModelOfferingStatus(ChangeModelOfferingStatusCommand command);
}
