package com.myagent.common.web;

import com.myagent.common.error.BizException;
import com.myagent.common.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 全局异常处理测试。
 */
class GlobalExceptionHandlerTests {

    /**
     * 验证业务异常能保留错误码和结构化明细。
     */
    @Test
    void handleBizExceptionKeepsErrorCodeAndDetails() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        BizException exception = new BizException(
                ErrorCode.AGENT_NOT_FOUND,
                "指定 Agent 不存在。",
                List.of(new com.myagent.common.api.ApiError.Detail("$.agentId", "not_found", "指定 Agent 不存在。", null, "123", null))
        );

        ResponseEntity<com.myagent.common.api.ApiResponse<Void>> response = handler.handleBizException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getError().getCode()).isEqualTo("AGENT_NOT_FOUND");
        assertThat(response.getBody().getError().getDetails()).hasSize(1);
    }

    /**
     * 验证字段校验异常会返回结构化错误明细。
     *
     * @throws Exception 构造测试异常失败时抛出
     */
    @Test
    void handleMethodArgumentNotValidReturnsStructuredDetails() throws Exception {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        Method method = SampleRequest.class.getDeclaredMethod("validate", String.class);

        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new SampleRequest(), "sampleRequest");
        bindingResult.addError(new FieldError("sampleRequest", "name", null, false, null, null, "请输入名称"));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(new MethodParameter(method, 0), bindingResult);

        ResponseEntity<com.myagent.common.api.ApiResponse<Void>> response = handler.handleMethodArgumentNotValid(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError().getCode()).isEqualTo("INVALID_ARGUMENT");
        assertThat(response.getBody().getError().getDetails()).isNotEmpty();
    }

    /**
     * 测试请求对象。
     */
    static class SampleRequest {
        void validate(String name) {
        }
    }
}
