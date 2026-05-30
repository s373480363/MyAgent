package com.myagent.common.page;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.stream.Collectors;
import java.util.function.Function;

/**
 * 统一分页结果对象。
 *
 * @param <T> 列表项类型
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "PageResult", description = "统一分页结果对象。")
public final class PageResult<T> {

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
     * 构造分页结果对象。
     *
     * @param items 当前页数据
     * @param page 页码
     * @param pageSize 每页条数
     * @param total 总记录数
     */
    public PageResult(List<T> items, long page, long pageSize, long total) {
        this.items = items == null ? List.of() : List.copyOf(items);
        this.page = page;
        this.pageSize = pageSize;
        this.total = total;
    }

    /**
     * 快速构造分页结果对象。
     *
     * @param items 当前页数据
     * @param page 页码
     * @param pageSize 每页条数
     * @param total 总记录数
     * @param <T> 列表项类型
     * @return 分页结果对象
     */
    public static <T> PageResult<T> of(List<T> items, long page, long pageSize, long total) {
        return new PageResult<>(items, page, pageSize, total);
    }

    /**
     * 返回空分页结果。
     *
     * @param page 页码
     * @param pageSize 每页条数
     * @param <T> 列表项类型
     * @return 空分页结果
     */
    public static <T> PageResult<T> empty(long page, long pageSize) {
        return new PageResult<>(List.of(), page, pageSize, 0L);
    }

    /**
     * 转换列表项类型。
     *
     * @param mapper 项转换函数
     * @param <R> 目标列表项类型
     * @return 转换后的分页结果
     */
    public <R> PageResult<R> map(Function<? super T, ? extends R> mapper) {
        List<R> mappedItems = items.stream()
                .map(mapper::apply)
                .collect(Collectors.toList());
        return new PageResult<>(mappedItems, page, pageSize, total);
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
