package com.example.seckill.monitor;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 性能压测服务
 * 
 * 提供简单的 API 压力测试功能：
 * - 并发请求模拟
 * - QPS/响应时间统计
 * - 压测报告生成
 */
@Service
public class PerformanceTestService {

    private final ExecutorService executorService;

    public PerformanceTestService() {
        // 创建压测专用线程池
        this.executorService = Executors.newFixedThreadPool(100);
    }

    /**
     * 执行 HTTP 压力测试
     * 
     * @param url        目标 URL
     * @param concurrent 并发数
     * @param requests   总请求数
     * @param method     HTTP 方法 (GET/POST)
     */
    public Map<String, Object> runHttpStressTest(String url, int concurrent,
            int requests, String method) {
        Map<String, Object> result = new LinkedHashMap<>();

        // 参数校验
        if (url == null || url.isEmpty()) {
            result.put("error", "URL 不能为空");
            return result;
        }
        concurrent = Math.min(concurrent, 100); // 限制最大并发
        requests = Math.min(requests, 10000); // 限制最大请求数

        result.put("target_url", url);
        result.put("concurrent", concurrent);
        result.put("total_requests", requests);
        result.put("method", method);

        RestTemplate restTemplate = new RestTemplate();

        // 统计指标
        AtomicLong successCount = new AtomicLong(0);
        AtomicLong failCount = new AtomicLong(0);
        List<Long> responseTimes = Collections.synchronizedList(new ArrayList<>());

        // 使用 CountDownLatch 控制并发
        CountDownLatch latch = new CountDownLatch(requests);

        long startTime = System.currentTimeMillis();

        // 提交请求
        for (int i = 0; i < requests; i++) {
            executorService.submit(() -> {
                long reqStart = System.nanoTime();
                try {
                    if ("POST".equalsIgnoreCase(method)) {
                        restTemplate.postForObject(url, null, String.class);
                    } else {
                        restTemplate.getForObject(url, String.class);
                    }
                    successCount.incrementAndGet();
                    long reqTime = (System.nanoTime() - reqStart) / 1_000_000;
                    responseTimes.add(reqTime);
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        // 等待所有请求完成
        try {
            boolean completed = latch.await(60, TimeUnit.SECONDS);
            if (!completed) {
                result.put("warning", "部分请求超时");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            result.put("error", "压测被中断");
        }

        long totalTime = System.currentTimeMillis() - startTime;

        // 计算统计信息
        result.put("total_time_ms", totalTime);
        result.put("success_count", successCount.get());
        result.put("fail_count", failCount.get());
        result.put("success_rate", String.format("%.2f%%",
                (double) successCount.get() / requests * 100));

        // QPS 计算
        double qps = totalTime > 0 ? (double) requests * 1000 / totalTime : 0;
        result.put("qps", String.format("%.2f", qps));

        // 响应时间统计
        if (!responseTimes.isEmpty()) {
            Collections.sort(responseTimes);
            result.put("response_time", calculateResponseTimeStats(responseTimes));
        }

        return result;
    }

    /**
     * 计算响应时间统计
     */
    private Map<String, Object> calculateResponseTimeStats(List<Long> times) {
        Map<String, Object> stats = new LinkedHashMap<>();

        if (times.isEmpty()) {
            return stats;
        }

        // 基础统计
        long sum = times.stream().mapToLong(Long::longValue).sum();
        stats.put("avg_ms", String.format("%.2f", (double) sum / times.size()));
        stats.put("min_ms", times.get(0));
        stats.put("max_ms", times.get(times.size() - 1));

        // 百分位数
        stats.put("p50_ms", getPercentile(times, 50));
        stats.put("p90_ms", getPercentile(times, 90));
        stats.put("p95_ms", getPercentile(times, 95));
        stats.put("p99_ms", getPercentile(times, 99));

        return stats;
    }

    private long getPercentile(List<Long> sortedTimes, int percentile) {
        int index = (int) Math.ceil(percentile / 100.0 * sortedTimes.size()) - 1;
        return sortedTimes.get(Math.max(0, index));
    }

    /**
     * 内存压力测试 - 模拟内存分配
     */
    public Map<String, Object> runMemoryStressTest(int sizeMb, int durationSeconds) {
        Map<String, Object> result = new LinkedHashMap<>();

        sizeMb = Math.min(sizeMb, 512); // 限制最大 512MB
        durationSeconds = Math.min(durationSeconds, 30); // 限制最大 30 秒

        result.put("target_size_mb", sizeMb);
        result.put("duration_seconds", durationSeconds);

        Runtime runtime = Runtime.getRuntime();
        long beforeUsed = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;

        try {
            // 分配内存
            byte[][] memory = new byte[sizeMb][];
            for (int i = 0; i < sizeMb; i++) {
                memory[i] = new byte[1024 * 1024]; // 1MB 每块
            }

            long afterUsed = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;

            result.put("memory_before_mb", beforeUsed);
            result.put("memory_after_mb", afterUsed);
            result.put("memory_allocated_mb", afterUsed - beforeUsed);

            // 保持一段时间
            Thread.sleep(durationSeconds * 1000L);

            // 释放内存
            memory = null;
            System.gc();

            Thread.sleep(1000);
            long afterGc = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
            result.put("memory_after_gc_mb", afterGc);

        } catch (OutOfMemoryError e) {
            result.put("error", "内存不足: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            result.put("error", "测试被中断");
        }

        return result;
    }

    /**
     * CPU 压力测试
     */
    public Map<String, Object> runCpuStressTest(int threads, int durationSeconds) {
        Map<String, Object> result = new LinkedHashMap<>();

        threads = Math.min(threads, Runtime.getRuntime().availableProcessors() * 2);
        durationSeconds = Math.min(durationSeconds, 30);

        result.put("threads", threads);
        result.put("duration_seconds", durationSeconds);
        result.put("cpu_cores", Runtime.getRuntime().availableProcessors());

        CountDownLatch latch = new CountDownLatch(threads);
        AtomicLong totalIterations = new AtomicLong(0);

        long startTime = System.currentTimeMillis();
        long endTime = startTime + durationSeconds * 1000L;

        for (int i = 0; i < threads; i++) {
            executorService.submit(() -> {
                long iterations = 0;
                while (System.currentTimeMillis() < endTime) {
                    // CPU 密集型计算
                    double x = Math.random();
                    for (int j = 0; j < 1000; j++) {
                        x = Math.sin(x) * Math.cos(x);
                    }
                    iterations++;
                }
                totalIterations.addAndGet(iterations);
                latch.countDown();
            });
        }

        try {
            latch.await(durationSeconds + 5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long actualDuration = System.currentTimeMillis() - startTime;
        result.put("actual_duration_ms", actualDuration);
        result.put("total_iterations", totalIterations.get());
        result.put("iterations_per_second", totalIterations.get() * 1000 / actualDuration);

        return result;
    }
}
