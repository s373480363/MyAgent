package com.myagent.settings.repository;

import com.myagent.common.repository.InstantTypeHandler;
import com.myagent.settings.domain.SettingValueType;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 系统设置 MyBatis Mapper。
 */
@Mapper
public interface SystemSettingMapper {

    /**
     * 按设置键查询记录。
     *
     * @param keys 设置键列表
     * @return 记录列表
     */
    @ConstructorArgs({
            @Arg(column = "setting_key", javaType = String.class),
            @Arg(column = "setting_value", javaType = String.class),
            @Arg(column = "value_type", javaType = SettingValueType.class),
            @Arg(column = "description", javaType = String.class),
            @Arg(column = "editable", javaType = boolean.class),
            @Arg(column = "updated_at", javaType = java.time.Instant.class, typeHandler = InstantTypeHandler.class)
    })
    @Select({
            "<script>",
            "select setting_key, setting_value, value_type, description, editable, updated_at",
            "from system_setting",
            "where setting_key in",
            "<foreach item='key' collection='keys' open='(' separator=',' close=')'>",
            "#{key}",
            "</foreach>",
            "</script>"
    })
    List<SystemSettingRecord> findByKeys(@Param("keys") List<String> keys);

    /**
     * 插入或更新系统设置。
     *
     * @param record 系统设置记录
     * @return 受影响行数
     */
    @Insert("""
            insert into system_setting(setting_key, setting_value, value_type, description, editable)
            values (
              #{record.settingKey},
              #{record.settingValue},
              #{record.valueType},
              #{record.description},
              #{record.editable}
            )
            on conflict (setting_key) do update
            set setting_value = excluded.setting_value,
                value_type = excluded.value_type,
                description = excluded.description,
                editable = excluded.editable,
                updated_at = now()
            """)
    int upsert(@Param("record") SystemSettingRecord record);
}
