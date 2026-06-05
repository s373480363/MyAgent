package com.myagent.modelcatalog.repository;

import com.myagent.common.domain.EnableStatus;
import com.myagent.common.page.PageResult;
import com.myagent.modelcatalog.application.query.ListModelProvidersQuery;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 基于 MyBatis 的模型供应商仓储实现。
 */
@Repository
public class MyBatisModelProviderRepository implements ModelProviderRepository {

    /**
     * MyBatis Mapper。
     */
    private final ModelProviderMapper modelProviderMapper;

    /**
     * 构造模型供应商仓储。
     *
     * @param modelProviderMapper MyBatis Mapper
     */
    public MyBatisModelProviderRepository(ModelProviderMapper modelProviderMapper) {
        this.modelProviderMapper = modelProviderMapper;
    }

    @Override
    public PageResult<ModelProviderRecord> listProviders(ListModelProvidersQuery query) {
        long offset = (query.page() - 1L) * query.pageSize();
        return PageResult.of(
                modelProviderMapper.listProviders(query.keyword(), query.status(), query.pageSize(), offset),
                query.page(),
                query.pageSize(),
                modelProviderMapper.countProviders(query.keyword(), query.status())
        );
    }

    @Override
    public Optional<ModelProviderRecord> findById(long providerId) {
        return Optional.ofNullable(modelProviderMapper.findById(providerId));
    }

    @Override
    public Optional<ModelProviderRecord> findByProviderKey(String providerKey) {
        return Optional.ofNullable(modelProviderMapper.findByProviderKey(providerKey));
    }

    @Override
    public ModelProviderRecord insert(ModelProviderRecord record) {
        return modelProviderMapper.insert(record);
    }

    @Override
    public int update(ModelProviderRecord record) {
        return modelProviderMapper.update(record);
    }

    @Override
    public int updateStatus(long providerId, EnableStatus status) {
        return modelProviderMapper.updateStatus(providerId, status);
    }
}
