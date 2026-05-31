package com.myagent.method.application.result;

import com.myagent.common.domain.EnableStatus;

import java.time.Instant;

/**
 * Java 方法详情结果。
 *
 * @param id 主键
 * @param methodKey 方法标识
 * @param name 方法名称
 * @param description 方法描述
 * @param beanName Bean 名称
 * @param methodName 方法名
 * @param inputSchemaId 输入 Schema 主键
 * @param outputSchemaId 输出 Schema 主键
 * @param status 状态
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record JavaMethodDetailResult(
        long id,
        String methodKey,
        String name,
        String description,
        String beanName,
        String methodName,
        long inputSchemaId,
        long outputSchemaId,
        EnableStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
