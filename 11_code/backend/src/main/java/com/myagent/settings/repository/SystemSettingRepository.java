package com.myagent.settings.repository;

import java.util.List;
import java.util.Map;

/**
 * 系统设置仓储接口。
 */
public interface SystemSettingRepository {

    /**
     * 按键查询系统设置。
     *
     * @param settingKeys 设置键列表
     * @return 以设置键为索引的结果映射
     */
    Map<String, SystemSettingRecord> findByKeys(List<String> settingKeys);

    /**
     * 插入或更新系统设置。
     *
     * @param record 系统设置记录
     */
    void upsert(SystemSettingRecord record);
}
