package com.myagent.modelcatalog.repository;

import com.myagent.common.domain.EnableStatus;
import com.myagent.common.page.PageResult;
import com.myagent.modelcatalog.application.query.ListModelOfferingsQuery;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 基于 MyBatis 的模型供应项仓储实现。
 */
@Repository
public class MyBatisModelOfferingRepository implements ModelOfferingRepository {

    /**
     * MyBatis Mapper。
     */
    private final ModelOfferingMapper modelOfferingMapper;

    /**
     * 构造模型供应项仓储。
     *
     * @param modelOfferingMapper MyBatis Mapper
     */
    public MyBatisModelOfferingRepository(ModelOfferingMapper modelOfferingMapper) {
        this.modelOfferingMapper = modelOfferingMapper;
    }

    @Override
    public PageResult<ModelOfferingJoinedRecord> listOfferings(ListModelOfferingsQuery query) {
        long offset = (query.page() - 1L) * query.pageSize();
        return PageResult.of(
                modelOfferingMapper.listOfferings(query.providerKey(), query.keyword(), query.status(), query.pageSize(), offset),
                query.page(),
                query.pageSize(),
                modelOfferingMapper.countOfferings(query.providerKey(), query.keyword(), query.status())
        );
    }

    @Override
    public Optional<ModelOfferingRecord> findById(long offeringId) {
        return Optional.ofNullable(modelOfferingMapper.findById(offeringId));
    }

    @Override
    public Optional<ModelOfferingRecord> findByOfferingKey(String offeringKey) {
        return Optional.ofNullable(modelOfferingMapper.findByOfferingKey(offeringKey));
    }

    @Override
    public Optional<ModelOfferingJoinedRecord> findJoinedByOfferingKey(String offeringKey) {
        return Optional.ofNullable(modelOfferingMapper.findJoinedByOfferingKey(offeringKey));
    }

    @Override
    public List<ModelOfferingJoinedRecord> findJoinedByOfferingKeys(List<String> offeringKeys) {
        if (offeringKeys == null || offeringKeys.isEmpty()) {
            return List.of();
        }
        return modelOfferingMapper.findJoinedByOfferingKeys(offeringKeys);
    }

    @Override
    public Optional<ModelOfferingRecord> findByProviderKeyAndUpstreamModelName(String providerKey, String upstreamModelName) {
        return Optional.ofNullable(modelOfferingMapper.findByProviderKeyAndUpstreamModelName(providerKey, upstreamModelName));
    }

    @Override
    public ModelOfferingRecord insert(ModelOfferingRecord record) {
        return modelOfferingMapper.insert(record);
    }

    @Override
    public int update(ModelOfferingRecord record) {
        return modelOfferingMapper.update(record);
    }

    @Override
    public int updateStatus(long offeringId, EnableStatus status) {
        return modelOfferingMapper.updateStatus(offeringId, status);
    }
}
