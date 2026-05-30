package com.myagent.schema.application.result;

import com.myagent.common.page.PageResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Schema 分页响应。
 */
@Schema(name = "SchemaPageResponse", description = "Schema 分页响应。")
public final class SchemaPageResponse {

    /**
     * 当前页数据。
     */
    @Schema(description = "当前页数据。", requiredMode = Schema.RequiredMode.REQUIRED)
    private final List<SchemaListItemResult> items;

    /**
     * 页码。
     */
    @Schema(description = "页码，从 1 开始。", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private final long page;

    /**
     * 每页条数。
     */
    @Schema(description = "每页条数。", example = "20", requiredMode = Schema.RequiredMode.REQUIRED)
    private final long pageSize;

    /**
     * 总记录数。
     */
    @Schema(description = "总记录数。", example = "0", requiredMode = Schema.RequiredMode.REQUIRED)
    private final long total;

    /**
     * 构造 Schema 分页响应。
     *
     * @param items 当前页数据
     * @param page 页码
     * @param pageSize 每页条数
     * @param total 总记录数
     */
    public SchemaPageResponse(List<SchemaListItemResult> items, long page, long pageSize, long total) {
        this.items = items == null ? List.of() : List.copyOf(items);
        this.page = page;
        this.pageSize = pageSize;
        this.total = total;
    }

    /**
     * 由应用层分页结果转换。
     *
     * @param pageResult 应用层分页结果
     * @return Schema 分页响应
     */
    public static SchemaPageResponse from(PageResult<SchemaListItemResult> pageResult) {
        return new SchemaPageResponse(
                pageResult.getItems(),
                pageResult.getPage(),
                pageResult.getPageSize(),
                pageResult.getTotal()
        );
    }

    public List<SchemaListItemResult> getItems() {
        return items;
    }

    public long getPage() {
        return page;
    }

    public long getPageSize() {
        return pageSize;
    }

    public long getTotal() {
        return total;
    }
}
