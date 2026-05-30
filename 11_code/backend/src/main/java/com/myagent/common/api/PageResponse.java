package com.myagent.common.api;

import io.swagger.v3.oas.annotations.media.Schema;
import com.myagent.common.page.PageResult;

import java.util.List;

/**
 * 统一分页响应对象。
 *
 * @param <T> 列表项类型
 */
@Schema(name = "PageResponse", description = "统一分页响应对象。")
public final class PageResponse<T> {

    /**
     * 当前页数据。
     */
    @Schema(description = "当前页数据。", requiredMode = Schema.RequiredMode.REQUIRED)
    private final List<T> items;

    /**
     * 页码，从 1 开始。
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
     * 构造分页响应对象。
     *
     * @param items 当前页数据
     * @param page 页码
     * @param pageSize 每页条数
     * @param total 总记录数
     */
    public PageResponse(List<T> items, long page, long pageSize, long total) {
        this.items = items == null ? List.of() : List.copyOf(items);
        this.page = page;
        this.pageSize = pageSize;
        this.total = total;
    }

    /**
     * 快速构造分页响应对象。
     *
     * @param items 当前页数据
     * @param page 页码
     * @param pageSize 每页条数
     * @param total 总记录数
     * @param <T> 列表项类型
     * @return 分页响应对象
     */
    public static <T> PageResponse<T> of(List<T> items, long page, long pageSize, long total) {
        return new PageResponse<>(items, page, pageSize, total);
    }

    /**
     * 从应用层分页结果构造 REST 分页响应。
     *
     * @param pageResult 应用层分页结果
     * @param <T> 列表项类型
     * @return REST 分页响应
     */
    public static <T> PageResponse<T> from(PageResult<T> pageResult) {
        return new PageResponse<>(
                pageResult.getItems(),
                pageResult.getPage(),
                pageResult.getPageSize(),
                pageResult.getTotal()
        );
    }

    /**
     * 返回当前页数据。
     *
     * @return 当前页数据
     */
    public List<T> getItems() {
        return items;
    }

    /**
     * 返回页码。
     *
     * @return 页码
     */
    public long getPage() {
        return page;
    }

    /**
     * 返回每页条数。
     *
     * @return 每页条数
     */
    public long getPageSize() {
        return pageSize;
    }

    /**
     * 返回总记录数。
     *
     * @return 总记录数
     */
    public long getTotal() {
        return total;
    }
}
