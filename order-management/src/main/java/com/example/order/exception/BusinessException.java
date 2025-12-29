package com.example.order.exception;

/**
 * 业务异常
 */
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }

    public static BusinessException of(String message) {
        return new BusinessException(message);
    }

    public static BusinessException notFound(String message) {
        return new BusinessException(404, message);
    }
}
