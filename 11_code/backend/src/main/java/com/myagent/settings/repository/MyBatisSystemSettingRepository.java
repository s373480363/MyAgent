package com.myagent.settings.repository;

import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于 MyBatis 的系统设置仓储实现。
 */
@Repository
public class MyBatisSystemSettingRepository implements SystemSettingRepository {

    /**
     * MyBatis Mapper。
     */
    private final SystemSettingMapper systemSettingMapper;

    /**
     * 构造系统设置仓储。
     *
     * @param systemSettingMapper MyBatis Mapper
     */
    public MyBatisSystemSettingRepository(SystemSettingMapper systemSettingMapper) {
        this.systemSettingMapper = systemSettingMapper;
    }

    /**
     * 按键查询系统设置。
     *
     * @param settingKeys 设置键列表
     * @return 结果映射
     */
    @Override
    public Map<String, SystemSettingRecord> findByKeys(List<String> settingKeys) {
        if (settingKeys == null || settingKeys.isEmpty()) {
            return Map.of();
        }
        Map<String, SystemSettingRecord> records = new LinkedHashMap<>();
        systemSettingMapper.findByKeys(settingKeys)
                .forEach(record -> records.put(record.settingKey(), record));
        return records;
    }

    /**
     * 插入或更新系统设置。
     *
     * @param record 系统设置记录
     */
    @Override
    public void upsert(SystemSettingRecord record) {
        systemSettingMapper.upsert(record);
    }
}
