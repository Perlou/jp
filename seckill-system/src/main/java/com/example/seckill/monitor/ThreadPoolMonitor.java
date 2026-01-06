package com.example.seckill.monitor;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池监控服务
 * 
 * 线程池调优要点：
 * 1. CPU 密集型：线程数 = CPU 核心数 + 1
 * 2. IO 密集型：线程数 = CPU 核心数 × 2 (或更多)
 * 3. 使用有界队列防止 OOM
 * 4. 合理设置拒绝策略
 */
@Service
public class ThreadPoolMonitor {

    private final Map<String, ThreadPoolExecutor> threadPools = new LinkedHashMap<>();

    /**
     * 注册线程池以便监控
     */
    public void registerThreadPool(String name, ThreadPoolExecutor executor) {
        threadPools.put(name, executor);
    }

    /**
     * 获取所有注册的线程池状态
     */
    public Map<String, Object> getAllPoolStatus() {
        Map<String, Object> status = new LinkedHashMap<>();

        if (threadPools.isEmpty()) {
            status.put("message", "尚未注册任何线程池");
            status.put("usage_guide", "使用 registerThreadPool() 注册线程池");
            return status;
        }

        for (Map.Entry<String, ThreadPoolExecutor> entry : threadPools.entrySet()) {
            status.put(entry.getKey(), getPoolDetail(entry.getValue()));
        }

        return status;
    }

    /**
     * 获取单个线程池详情
     */
    public Map<String, Object> getPoolDetail(ThreadPoolExecutor executor) {
        Map<String, Object> detail = new LinkedHashMap<>();

        // 配置信息
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("core_pool_size", executor.getCorePoolSize());
        config.put("maximum_pool_size", executor.getMaximumPoolSize());
        config.put("keep_alive_time_seconds", executor.getKeepAliveTime(java.util.concurrent.TimeUnit.SECONDS));
        config.put("queue_type", executor.getQueue().getClass().getSimpleName());
        config.put("queue_capacity", getQueueCapacity(executor));
        config.put("rejected_handler", executor.getRejectedExecutionHandler().getClass().getSimpleName());
        detail.put("config", config);

        // 运行时状态
        Map<String, Object> runtime = new LinkedHashMap<>();
        runtime.put("pool_size", executor.getPoolSize());
        runtime.put("active_count", executor.getActiveCount());
        runtime.put("queue_size", executor.getQueue().size());
        runtime.put("completed_task_count", executor.getCompletedTaskCount());
        runtime.put("task_count", executor.getTaskCount());
        runtime.put("largest_pool_size", executor.getLargestPoolSize());
        detail.put("runtime", runtime);

        // 使用率
        double usage = (double) executor.getActiveCount() / executor.getMaximumPoolSize() * 100;
        detail.put("usage_percent", String.format("%.1f%%", usage));

        // 状态判断
        List<String> suggestions = new ArrayList<>();
        if (usage > 80) {
            suggestions.add("⚠️ 线程池使用率超过 80%");
        }
        if (executor.getQueue().size() > 0) {
            suggestions.add("📋 队列中有 " + executor.getQueue().size() + " 个任务等待执行");
        }
        if (executor.getLargestPoolSize() == executor.getMaximumPoolSize()) {
            suggestions.add("📈 线程池曾达到最大容量");
        }
        if (suggestions.isEmpty()) {
            suggestions.add("✅ 线程池状态正常");
        }
        detail.put("suggestions", suggestions);

        return detail;
    }

    /**
     * 获取线程池最佳配置建议
     */
    public Map<String, Object> getRecommendedConfig() {
        int cpuCores = Runtime.getRuntime().availableProcessors();

        Map<String, Object> recommendations = new LinkedHashMap<>();
        recommendations.put("cpu_cores", cpuCores);

        // CPU 密集型任务配置
        Map<String, Object> cpuIntensive = new LinkedHashMap<>();
        cpuIntensive.put("core_pool_size", cpuCores + 1);
        cpuIntensive.put("maximum_pool_size", cpuCores + 1);
        cpuIntensive.put("queue_capacity", 100);
        cpuIntensive.put("scenario", "计算密集型任务：图片处理、数据计算等");
        cpuIntensive.put("reason", "CPU 密集型任务不需要太多线程，过多线程反而增加上下文切换开销");
        recommendations.put("cpu_intensive", cpuIntensive);

        // IO 密集型任务配置
        Map<String, Object> ioIntensive = new LinkedHashMap<>();
        ioIntensive.put("core_pool_size", cpuCores * 2);
        ioIntensive.put("maximum_pool_size", cpuCores * 4);
        ioIntensive.put("queue_capacity", 1000);
        ioIntensive.put("scenario", "IO 密集型任务：网络请求、数据库操作等");
        ioIntensive.put("reason", "IO 等待时线程会阻塞，需要更多线程来提高 CPU 利用率");
        recommendations.put("io_intensive", ioIntensive);

        // 拒绝策略说明
        Map<String, String> rejectionPolicies = new LinkedHashMap<>();
        rejectionPolicies.put("AbortPolicy", "抛出异常（默认）");
        rejectionPolicies.put("CallerRunsPolicy", "调用者线程执行（推荐，可降速）");
        rejectionPolicies.put("DiscardPolicy", "静默丢弃");
        rejectionPolicies.put("DiscardOldestPolicy", "丢弃最旧任务");
        recommendations.put("rejection_policies", rejectionPolicies);

        return recommendations;
    }

    private int getQueueCapacity(ThreadPoolExecutor executor) {
        if (executor.getQueue() instanceof java.util.concurrent.LinkedBlockingQueue) {
            return ((java.util.concurrent.LinkedBlockingQueue<?>) executor.getQueue())
                    .remainingCapacity() + executor.getQueue().size();
        }
        return -1; // 无界队列
    }
}
