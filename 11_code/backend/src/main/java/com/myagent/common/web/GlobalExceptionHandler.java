package com.myagent.common.web;

import com.myagent.common.api.ApiError;
import com.myagent.common.api.ApiResponse;
import com.myagent.common.error.BizException;
import com.myagent.common.error.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;

/**
 * 全局异常处理器。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 日志记录器。
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理业务异常。
     *
     * @param exception 业务异常
     * @return 统一失败响应
     */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResponse<Void>> handleBizException(BizException exception) {
        // 业务异常已经具备稳定错误码和中文消息，直接透传给前端。
        ApiError error = ApiError.of(
                exception.getErrorCode().getCode(),
                exception.getMessage(),
                exception.getDetails()
        );
        return ResponseEntity.ok(ApiResponse.failure(error));
    }

    /**
     * 处理请求对象字段校验失败。
     *
     * @param exception 参数校验异常
     * @return 统一失败响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        // 收敛字段级校验错误，确保前端可以直接定位问题输入。
        List<ApiError.Detail> details = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::mapFieldError)
                .toList();
        ApiError error = ApiError.of(
                ErrorCode.INVALID_ARGUMENT.getCode(),
                "请求参数校验失败，请检查输入内容。",
                details
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.failure(error));
    }

    /**
     * 处理路径参数、查询参数等约束校验失败。
     *
     * @param exception 约束校验异常
     * @return 统一失败响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException exception) {
        // 将约束校验结果统一转换为字段路径 + 中文提示的结构，避免前端只能展示原始异常。
        List<ApiError.Detail> details = exception.getConstraintViolations()
                .stream()
                .map(this::mapConstraintViolation)
                .toList();
        ApiError error = ApiError.of(
                ErrorCode.INVALID_ARGUMENT.getCode(),
                "请求参数校验失败，请检查输入内容。",
                details
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.failure(error));
    }

    /**
     * 处理请求体无法解析的情况。
     *
     * @param exception 请求体解析异常
     * @return 统一失败响应
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException exception) {
        // 请求体解析错误属于前置失败，需要直接给出明确中文提示。
        ApiError error = ApiError.of(
                ErrorCode.INVALID_REQUEST_BODY.getCode(),
                "请求体格式不正确，请检查 JSON 结构和字段类型。"
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.failure(error));
    }

    /**
     * 处理未捕获的系统异常。
     *
     * @param exception 未捕获异常
     * @return 统一失败响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception exception) {
        LOGGER.error("未捕获的系统异常。", exception);
        // 对未知异常统一收口，避免直接把无意义堆栈信息暴露给调用方。
        ApiError error = ApiError.of(
                ErrorCode.INTERNAL_ERROR.getCode(),
                ErrorCode.INTERNAL_ERROR.getDefaultMessage()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.failure(error));
    }

    /**
     * 将字段错误转换为标准错误明细。
     *
     * @param fieldError Spring 字段错误
     * @return 标准错误明细
     */
    private ApiError.Detail mapFieldError(FieldError fieldError) {
        // 统一使用 $.fieldPath 形式表达字段定位，便于前端直接复用。
        String fieldPath = "$." + fieldError.getField();
        String message = fieldError.getDefaultMessage() == null ? "字段校验失败。" : fieldError.getDefaultMessage();
        return new ApiError.Detail(fieldPath, "invalid", message, null, stringify(fieldError.getRejectedValue()), null);
    }

    /**
     * 将约束校验错误转换为标准错误明细。
     *
     * @param violation Jakarta 约束错误
     * @return 标准错误明细
     */
    private ApiError.Detail mapConstraintViolation(ConstraintViolation<?> violation) {
        // 查询参数和路径参数没有 JSON 指针语义，这里仍然转成统一的 $.path 表达方式。
        String fieldPath = "$." + violation.getPropertyPath();
        return new ApiError.Detail(fieldPath, "invalid", violation.getMessage(), null, stringify(violation.getInvalidValue()), null);
    }

    /**
     * 安全地将对象转换为字符串。
     *
     * @param value 原始对象
     * @return 字符串值
     */
    private String stringify(Object value) {
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }
}
