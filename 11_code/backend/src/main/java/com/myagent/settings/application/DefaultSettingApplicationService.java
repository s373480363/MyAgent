package com.myagent.settings.application;

import com.myagent.common.error.BizException;
import com.myagent.common.error.ErrorCode;
import com.myagent.config.MyAgentSettingsProperties;
import com.myagent.settings.application.command.UpdateSettingsCommand;
import com.myagent.settings.application.result.SettingItemResult;
import com.myagent.settings.domain.SettingCatalogItem;
import com.myagent.settings.domain.SettingSource;
import com.myagent.settings.domain.SettingsCatalog;
import com.myagent.settings.repository.SystemSettingRecord;
import com.myagent.settings.repository.SystemSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 系统设置应用服务默认实现。
 */
@Service
public class DefaultSettingApplicationService implements SettingApplicationService {

    /**
     * 系统设置仓储。
     */
    private final SystemSettingRepository systemSettingRepository;

    /**
     * 白名单目录。
     */
    private final SettingsCatalog settingsCatalog;

    /**
     * 启动配置对象。
     */
    private final MyAgentSettingsProperties settingsProperties;

    /**
     * 构造系统设置应用服务。
     *
     * @param systemSettingRepository 系统设置仓储
     * @param settingsCatalog 白名单目录
     * @param settingsProperties 启动配置对象
     */
    public DefaultSettingApplicationService(
            SystemSettingRepository systemSettingRepository,
            SettingsCatalog settingsCatalog,
            MyAgentSettingsProperties settingsProperties
    ) {
        this.systemSettingRepository = systemSettingRepository;
        this.settingsCatalog = settingsCatalog;
        this.settingsProperties = settingsProperties;
    }

    /**
     * 读取系统设置列表。
     *
     * @return 系统设置列表
     */
    @Override
    public List<SettingItemResult> getSettings() {
        List<SettingCatalogItem> items = settingsCatalog.listItems();
        Map<String, SystemSettingRecord> records = systemSettingRepository.findByKeys(
                items.stream().map(SettingCatalogItem::getSettingKey).toList()
        );
        return items.stream()
                .map(item -> toSettingItemResult(item, records.get(item.getSettingKey())))
                .toList();
    }

    /**
     * 批量更新系统设置。
     *
     * @param command 批量更新命令
     */
    @Override
    @Transactional
    public void updateSettings(UpdateSettingsCommand command) {
        if (command == null || command.items() == null || command.items().isEmpty()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "至少需要提供一个待更新的系统设置项。");
        }
        for (UpdateSettingsCommand.Item item : command.items()) {
            SettingCatalogItem catalogItem = settingsCatalog.getRequired(item.settingKey());
            settingsCatalog.validate(catalogItem, item.valueType(), item.settingValue());
            systemSettingRepository.upsert(new SystemSettingRecord(
                    catalogItem.getSettingKey(),
                    item.settingValue().trim(),
                    catalogItem.getValueType(),
                    catalogItem.getDescription(),
                    catalogItem.isEditable(),
                    null
            ));
        }
    }

    /**
     * 转换单个系统设置结果。
     *
     * @param catalogItem 白名单项
     * @param record 数据库覆盖记录
     * @return 返回结果
     */
    private SettingItemResult toSettingItemResult(SettingCatalogItem catalogItem, SystemSettingRecord record) {
        if (record != null) {
            return new SettingItemResult(
                    catalogItem.getSettingKey(),
                    record.settingValue(),
                    record.valueType(),
                    record.editable(),
                    record.description(),
                    SettingSource.SYSTEM_SETTING
            );
        }
        return new SettingItemResult(
                catalogItem.getSettingKey(),
                catalogItem.resolveApplicationValue(settingsProperties),
                catalogItem.getValueType(),
                catalogItem.isEditable(),
                catalogItem.getDescription(),
                SettingSource.APPLICATION_CONFIG
        );
    }
}
