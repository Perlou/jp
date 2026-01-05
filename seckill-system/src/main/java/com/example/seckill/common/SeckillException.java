package com.example.seckill.common;

/**
 * 秒杀业务异常
 */
public class SeckillException extends RuntimeException {

    private int code;

    public SeckillException(String message) {
        super(message);
        this.code = Result.FAIL;
    }

    public SeckillException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
