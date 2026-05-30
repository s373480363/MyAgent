package com.myagent.common.api;

import com.myagent.common.error.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 公共契约对象测试。
 */
class ApiContractTests {

    /**
     * 验证 ApiError 可以承载结构化字段错误。
     */
    @Test
    void apiErrorPreservesStructuredDetails() {
        ApiError.Detail detail = new ApiError.Detail("$.input.question", "required", "请输入问题", "非空字符串", null, "补充必填字段");
        ApiError error = ApiError.of(ErrorCode.INVALID_ARGUMENT.getCode(), "请求参数不合法。", List.of(detail));

        assertThat(error.getCode()).isEqualTo("INVALID_ARGUMENT");
        assertThat(error.getMessage()).isEqualTo("请求参数不合法。");
        assertThat(error.getDetails()).containsExactly(detail);
    }

    /**
     * 验证 ApiResponse 成功和失败包装。
     */
    @Test
    void apiResponseWrapsSuccessAndFailure() {
        ApiResponse<String> success = ApiResponse.success("ok");
        ApiResponse<Void> failure = ApiResponse.failure(ApiError.of(ErrorCode.INTERNAL_ERROR.getCode(), "系统内部异常，请稍后重试。"));

        assertThat(success.isSuccess()).isTrue();
        assertThat(success.getData()).isEqualTo("ok");
        assertThat(success.getError()).isNull();

        assertThat(failure.isSuccess()).isFalse();
        assertThat(failure.getData()).isNull();
        assertThat(failure.getError().getCode()).isEqualTo("INTERNAL_ERROR");
    }

    /**
     * 验证分页结果可以稳定转换为 REST 分页响应。
     */
    @Test
    void pageResponseCanConvertFromPageResult() {
        com.myagent.common.page.PageResult<String> pageResult = com.myagent.common.page.PageResult.of(List.of("a", "b"), 2, 20, 100);
        PageResponse<String> pageResponse = PageResponse.from(pageResult);

        assertThat(pageResponse.getItems()).containsExactly("a", "b");
        assertThat(pageResponse.getPage()).isEqualTo(2);
        assertThat(pageResponse.getPageSize()).isEqualTo(20);
        assertThat(pageResponse.getTotal()).isEqualTo(100);
    }
}
