package com.example.seckill.config;

import com.example.seckill.monitor.ThreadPoolMonitor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 线程池配置
 * 
 * 最佳实践：
 * 1. CPU 密集型：核心数 + 1
 * 2. IO 密集型：核心数 × 2 或更多
 * 3. 使用有界队列
 * 4. 指定拒绝策略
 */
@Configuration
public class ThreadPoolConfig {

    private final ThreadPoolMonitor threadPoolMonitor;

    public ThreadPoolConfig(ThreadPoolMonitor threadPoolMonitor) {
        this.threadPoolMonitor = threadPoolMonitor;
    }

    /**
     * IO 密集型线程池
     * 用于：数据库操作、Redis 操作、HTTP 请求等
     */
    @Bean("ioThreadPool")
    public ThreadPoolExecutor ioThreadPool() {
        int cpuCores = Runtime.getRuntime().availableProcessors();

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                cpuCores * 2, // 核心线程数
                cpuCores * 4, // 最大线程数
                60L, TimeUnit.SECONDS, // 空闲线程存活时间
                new LinkedBlockingQueue<>(1000), // 有界队列
                new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略：调用者执行
        );

        // 注册到监控
        threadPoolMonitor.registerThreadPool("io-thread-pool", executor);

        return executor;
    }

    /**
     * CPU 密集型线程池
     * 用于：数据计算、图片处理等
     */
    @Bean("cpuThreadPool")
    public ThreadPoolExecutor cpuThreadPool() {
        int cpuCores = Runtime.getRuntime().availableProcessors();

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                cpuCores + 1, // 核心线程数
                cpuCores + 1, // 最大线程数 = 核心数（不扩展）
                0L, TimeUnit.SECONDS, // 不回收核心线程
                new LinkedBlockingQueue<>(100), // 有界队列
                new ThreadPoolExecutor.AbortPolicy() // 拒绝策略：抛异常
        );

        // 注册到监控
        threadPoolMonitor.registerThreadPool("cpu-thread-pool", executor);

        return executor;
    }
}
