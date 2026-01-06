package com.example.seckill.monitor;

import org.springframework.stereotype.Service;

import java.lang.management.*;
import java.util.*;

/**
 * JVM 监控服务
 * 
 * 提供 JVM 运行时信息监控：
 * - 堆内存/非堆内存使用
 * - GC 次数与耗时
 * - 线程状态统计
 * - CPU 信息
 */
@Service
public class JvmMonitorService {

    /**
     * 获取完整的 JVM 信息
     */
    public Map<String, Object> getJvmInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("memory", getMemoryInfo());
        info.put("gc", getGcInfo());
        info.put("thread", getThreadInfo());
        info.put("runtime", getRuntimeInfo());
        return info;
    }

    /**
     * 获取内存使用情况
     * 
     * 知识点：
     * - 堆内存 (Heap): 存放对象实例，由 GC 管理
     * - 非堆内存 (Non-Heap): 元空间(类信息)、代码缓存等
     */
    public Map<String, Object> getMemoryInfo() {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        Runtime runtime = Runtime.getRuntime();

        Map<String, Object> memory = new LinkedHashMap<>();

        // 堆内存
        MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
        Map<String, Object> heap = new LinkedHashMap<>();
        heap.put("init_mb", heapUsage.getInit() / 1024 / 1024);
        heap.put("used_mb", heapUsage.getUsed() / 1024 / 1024);
        heap.put("committed_mb", heapUsage.getCommitted() / 1024 / 1024);
        heap.put("max_mb", heapUsage.getMax() / 1024 / 1024);
        heap.put("usage_percent", String.format("%.2f%%",
                (double) heapUsage.getUsed() / heapUsage.getMax() * 100));
        memory.put("heap", heap);

        // 非堆内存 (Metaspace)
        MemoryUsage nonHeapUsage = memoryMXBean.getNonHeapMemoryUsage();
        Map<String, Object> nonHeap = new LinkedHashMap<>();
        nonHeap.put("used_mb", nonHeapUsage.getUsed() / 1024 / 1024);
        nonHeap.put("committed_mb", nonHeapUsage.getCommitted() / 1024 / 1024);
        memory.put("non_heap", nonHeap);

        // Runtime 信息
        memory.put("runtime_max_mb", runtime.maxMemory() / 1024 / 1024);
        memory.put("runtime_total_mb", runtime.totalMemory() / 1024 / 1024);
        memory.put("runtime_free_mb", runtime.freeMemory() / 1024 / 1024);

        return memory;
    }

    /**
     * 获取 GC 统计信息
     * 
     * 知识点：
     * - Young GC: 清理年轻代，频繁但快速
     * - Old GC (Full GC): 清理老年代，较慢需要关注
     */
    public Map<String, Object> getGcInfo() {
        Map<String, Object> gcInfo = new LinkedHashMap<>();
        List<Map<String, Object>> collectors = new ArrayList<>();

        long totalGcCount = 0;
        long totalGcTime = 0;

        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            Map<String, Object> gc = new LinkedHashMap<>();
            gc.put("name", gcBean.getName());
            gc.put("collection_count", gcBean.getCollectionCount());
            gc.put("collection_time_ms", gcBean.getCollectionTime());

            // 判断是 Young GC 还是 Old GC
            String name = gcBean.getName().toLowerCase();
            if (name.contains("young") || name.contains("minor") ||
                    name.contains("parnew") || name.contains("g1 young") ||
                    name.contains("copy") || name.contains("scavenge")) {
                gc.put("type", "Young GC");
            } else {
                gc.put("type", "Old GC");
            }

            collectors.add(gc);
            totalGcCount += gcBean.getCollectionCount();
            totalGcTime += gcBean.getCollectionTime();
        }

        gcInfo.put("collectors", collectors);
        gcInfo.put("total_gc_count", totalGcCount);
        gcInfo.put("total_gc_time_ms", totalGcTime);

        // GC 优化建议
        if (totalGcTime > 1000) {
            gcInfo.put("warning", "⚠️ GC 总时间超过 1 秒，建议检查内存配置");
        }

        return gcInfo;
    }

    /**
     * 获取线程信息
     * 
     * 知识点：
     * - RUNNABLE: 正在执行或等待 CPU
     * - BLOCKED: 等待获取锁
     * - WAITING: 等待其他线程通知
     * - TIMED_WAITING: 带超时的等待
     */
    public Map<String, Object> getThreadInfo() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

        Map<String, Object> threadInfo = new LinkedHashMap<>();
        threadInfo.put("thread_count", threadMXBean.getThreadCount());
        threadInfo.put("peak_thread_count", threadMXBean.getPeakThreadCount());
        threadInfo.put("daemon_thread_count", threadMXBean.getDaemonThreadCount());
        threadInfo.put("total_started_count", threadMXBean.getTotalStartedThreadCount());

        // 线程状态统计
        Map<Thread.State, Integer> stateCount = new EnumMap<>(Thread.State.class);
        for (Thread.State state : Thread.State.values()) {
            stateCount.put(state, 0);
        }

        long[] threadIds = threadMXBean.getAllThreadIds();
        ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(threadIds);
        for (ThreadInfo info : threadInfos) {
            if (info != null) {
                stateCount.merge(info.getThreadState(), 1, Integer::sum);
            }
        }
        threadInfo.put("state_distribution", stateCount);

        // 检测死锁
        long[] deadlockedThreads = threadMXBean.findDeadlockedThreads();
        if (deadlockedThreads != null && deadlockedThreads.length > 0) {
            threadInfo.put("deadlock_warning", "⚠️ 检测到死锁！死锁线程数: " + deadlockedThreads.length);
        }

        return threadInfo;
    }

    /**
     * 获取运行时信息
     */
    public Map<String, Object> getRuntimeInfo() {
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        OperatingSystemMXBean osMXBean = ManagementFactory.getOperatingSystemMXBean();

        Map<String, Object> runtime = new LinkedHashMap<>();
        runtime.put("jvm_name", runtimeMXBean.getVmName());
        runtime.put("jvm_version", runtimeMXBean.getVmVersion());
        runtime.put("java_version", System.getProperty("java.version"));
        runtime.put("uptime_seconds", runtimeMXBean.getUptime() / 1000);
        runtime.put("available_processors", osMXBean.getAvailableProcessors());
        runtime.put("system_load_average", osMXBean.getSystemLoadAverage());

        // JVM 启动参数
        runtime.put("input_arguments", runtimeMXBean.getInputArguments());

        return runtime;
    }

    /**
     * 手动触发 GC (仅用于测试)
     */
    public Map<String, Object> triggerGc() {
        Map<String, Object> before = getMemoryInfo();
        System.gc();
        Map<String, Object> after = getMemoryInfo();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("before", before);
        result.put("after", after);
        result.put("message", "⚠️ 手动 GC 已触发，生产环境请谨慎使用");
        return result;
    }
}
