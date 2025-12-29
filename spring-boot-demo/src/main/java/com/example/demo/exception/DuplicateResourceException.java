package com.example.demo.exception;

/**
 * 资源重复异常
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
