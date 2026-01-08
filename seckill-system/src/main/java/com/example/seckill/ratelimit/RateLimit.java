package com.example.seckill.ratelimit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 限流注解
 * 
 * 用于标注需要限流的方法或控制器
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 限流器名称（用于区分不同的限流规则）
     */
    String name() default "default";

    /**
     * 每秒允许的请求数 (QPS)
     */
    int qps() default 100;

    /**
     * 限流算法类型
     */
    Algorithm algorithm() default Algorithm.TOKEN_BUCKET;

    /**
     * 限流失败时的提示信息
     */
    String message() default "请求太频繁，请稍后再试";

    /**
     * 限流算法枚举
     */
    enum Algorithm {
        /**
         * 令牌桶 - 允许突发流量
         */
        TOKEN_BUCKET,

        /**
         * 滑动窗口 - 精确限流
         */
        SLIDING_WINDOW
    }
}
