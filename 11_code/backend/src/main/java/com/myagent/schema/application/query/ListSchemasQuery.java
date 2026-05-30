package com.myagent.schema.application.query;

import com.myagent.common.page.PageQuery;
import com.myagent.schema.domain.SchemaCreatedFrom;
import com.myagent.schema.domain.SchemaStatus;

/**
 * Schema 列表查询。
 *
 * @param page 页码
 * @param pageSize 每页条数
 * @param keyword 关键词
 * @param status 生命周期状态
 * @param createdFrom 来源
 */
public record ListSchemasQuery(
        long page,
        long pageSize,
        String keyword,
        SchemaStatus status,
        SchemaCreatedFrom createdFrom
) {

    /**
     * 创建默认分页查询。
     *
     * @return 默认分页查询
     */
    public static ListSchemasQuery ofDefault() {
        return new ListSchemasQuery(
                PageQuery.DEFAULT_PAGE,
                PageQuery.DEFAULT_PAGE_SIZE,
                null,
                null,
                null
        );
    }
}
