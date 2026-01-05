package com.example.seckill.common;

/**
 * 统一返回结果
 */
public class Result<T> {

    private int code;
    private String message;
    private T data;

    // 状态码常量
    public static final int SUCCESS = 200;
    public static final int FAIL = 400;
    public static final int ERROR = 500;

    public Result() {
    }

    public Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> success() {
        return new Result<>(SUCCESS, "success", null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(SUCCESS, "success", data);
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(SUCCESS, message, data);
    }

    public static <T> Result<T> fail(String message) {
        return new Result<>(FAIL, message, null);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> error(String message) {
        return new Result<>(ERROR, message, null);
    }

    public boolean isSuccess() {
        return code == SUCCESS;
    }

    // Getters and Setters
    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
