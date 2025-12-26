package com.example.jp_user_service.exception;

/**
 * 资源未找到异常
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
