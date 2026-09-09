package com.wwfinance.common.exception;

/**
 * 业务异常：Service 层校验失败时抛出，由 GlobalExceptionHandler 统一转为 PccAjaxResult，
 * Controller 不再需要逐段 if-return 处理错误
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        this(500, message);
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
