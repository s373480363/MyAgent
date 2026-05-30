package com.myagent.common.page;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.myagent.common.util.ValidationUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

/**
 * 统一分页查询对象。
 */
@Schema(name = "PageQuery", description = "统一分页查询对象。")
public final class PageQuery {

    /**
     * 默认页码。
     */
    public static final long DEFAULT_PAGE = 1L;

    /**
     * 默认每页条数。
     */
    public static final long DEFAULT_PAGE_SIZE = 20L;

    /**
     * 页码，从 1 开始。
     */
    @Min(1)
    @Schema(description = "页码，从 1 开始。", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private final long page;

    /**
     * 每页条数。
     */
    @Min(1)
    @Schema(description = "每页条数。", example = "20", requiredMode = Schema.RequiredMode.REQUIRED)
    private final long pageSize;

    /**
     * 构造分页查询对象。
     *
     * @param page 页码
     * @param pageSize 每页条数
     */
    private PageQuery(long page, long pageSize) {
        this.page = ValidationUtils.requirePositive(page, "page");
        this.pageSize = ValidationUtils.requirePositive(pageSize, "pageSize");
    }

    /**
     * 创建分页查询对象。
     *
     * @param page 页码
     * @param pageSize 每页条数
     * @return 分页查询对象
     */
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public static PageQuery of(
            @JsonProperty("page") long page,
            @JsonProperty("pageSize") long pageSize
    ) {
        return new PageQuery(page, pageSize);
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
     * 返回数据库偏移量。
     *
     * @return 偏移量
     */
    public long getOffset() {
        return (page - 1) * pageSize;
    }
}
