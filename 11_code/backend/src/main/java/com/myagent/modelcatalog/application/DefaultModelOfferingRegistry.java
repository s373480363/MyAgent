package com.myagent.modelcatalog.application;

import com.myagent.common.domain.EnableStatus;
import com.myagent.common.page.PageResult;
import com.myagent.modelcatalog.application.query.ListModelOfferingsQuery;
import com.myagent.modelcatalog.application.result.ModelOfferingDescriptor;
import com.myagent.modelcatalog.repository.ModelOfferingJoinedRecord;
import com.myagent.modelcatalog.repository.ModelOfferingRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 默认模型供应项安全注册表。
 */
@Component
public class DefaultModelOfferingRegistry implements ModelOfferingRegistry {

    /**
     * 模型供应项仓储。
     */
    private final ModelOfferingRepository modelOfferingRepository;

    /**
     * 构造注册表。
     *
     * @param modelOfferingRepository 模型供应项仓储
     */
    public DefaultModelOfferingRegistry(ModelOfferingRepository modelOfferingRepository) {
        this.modelOfferingRepository = modelOfferingRepository;
    }

    @Override
    public PageResult<ModelOfferingDescriptor> listEnabled(ListModelOfferingsQuery query) {
        return modelOfferingRepository.listOfferings(query).map(this::toDescriptor);
    }

    @Override
    public Optional<ModelOfferingDescriptor> findByOfferingKey(String offeringKey) {
        return modelOfferingRepository.findJoinedByOfferingKey(offeringKey).map(this::toDescriptor);
    }

    @Override
    public List<ModelOfferingDescriptor> findByOfferingKeys(List<String> offeringKeys) {
        return modelOfferingRepository.findJoinedByOfferingKeys(offeringKeys).stream()
                .map(this::toDescriptor)
                .toList();
    }

    /**
     * 转换为安全描述。
     *
     * @param record 联表记录
     * @return 安全描述
     */
    private ModelOfferingDescriptor toDescriptor(ModelOfferingJoinedRecord record) {
        boolean selectable = record.status() == EnableStatus.ENABLED && record.providerStatus() == EnableStatus.ENABLED;
        return new ModelOfferingDescriptor(
                record.offeringId(),
                record.offeringKey(),
                record.providerKey(),
                record.providerName(),
                record.modelKey(),
                record.displayName(),
                record.upstreamModelName(),
                record.defaultTemperature(),
                record.status(),
                record.providerStatus(),
                selectable,
                selectable ? "" : unavailableReason(record.status(), record.providerStatus()),
                record.description(),
                record.createdAt(),
                record.updatedAt()
        );
    }

    /**
     * 生成不可选原因。
     *
     * @param offeringStatus 供应项状态
     * @param providerStatus 供应商状态
     * @return 中文原因
     */
    private String unavailableReason(EnableStatus offeringStatus, EnableStatus providerStatus) {
        if (providerStatus != EnableStatus.ENABLED) {
            return "所属模型供应商已停用";
        }
        if (offeringStatus != EnableStatus.ENABLED) {
            return "模型供应项已停用";
        }
        return "";
    }
}
