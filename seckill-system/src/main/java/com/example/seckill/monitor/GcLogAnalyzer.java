package com.example.seckill.monitor;

import org.springframework.stereotype.Service;

import java.lang.management.*;
import java.util.*;

/**
 * GC 日志分析器
 * 
 * 提供 GC 性能分析与优化建议：
 * - GC 类型识别
 * - GC 效率分析
 * - 内存趋势监控
 * - 优化建议生成
 */
@Service
public class GcLogAnalyzer {

    // 历史 GC 数据记录
    private final List<Map<String, Object>> gcHistory = Collections.synchronizedList(
            new ArrayList<>());
    private static final int MAX_HISTORY_SIZE = 100;

    private long lastGcCount = 0;
    private long lastGcTime = 0;

    /**
     * 获取 GC 详细分析报告
     */
    public Map<String, Object> getGcAnalysisReport() {
        Map<String, Object> report = new LinkedHashMap<>();

        // 收集当前 GC 信息
        Map<String, Object> currentGc = collectCurrentGcInfo();
        report.put("current_gc", currentGc);

        // 内存池分析
        report.put("memory_pools", analyzeMemoryPools());

        // GC 效率分析
        report.put("gc_efficiency", calculateGcEfficiency());

        // 优化建议
        report.put("recommendations", generateOptimizationRecommendations());

        // GC 历史趋势
        report.put("gc_trend", getGcTrend());

        return report;
    }

    /**
     * 收集当前 GC 信息
     */
    private Map<String, Object> collectCurrentGcInfo() {
        Map<String, Object> gcInfo = new LinkedHashMap<>();
        List<Map<String, Object>> collectors = new ArrayList<>();

        long totalGcCount = 0;
        long totalGcTime = 0;
        String youngGcName = "";
        String oldGcName = "";

        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            Map<String, Object> gc = new LinkedHashMap<>();
            String name = gcBean.getName();
            gc.put("name", name);
            gc.put("collection_count", gcBean.getCollectionCount());
            gc.put("collection_time_ms", gcBean.getCollectionTime());

            // 计算平均 GC 时间
            if (gcBean.getCollectionCount() > 0) {
                gc.put("avg_gc_time_ms", String.format("%.2f",
                        (double) gcBean.getCollectionTime() / gcBean.getCollectionCount()));
            }

            // 识别 GC 类型
            String lowerName = name.toLowerCase();
            if (lowerName.contains("young") || lowerName.contains("minor") ||
                    lowerName.contains("parnew") || lowerName.contains("copy") ||
                    lowerName.contains("scavenge") || lowerName.contains("g1 young")) {
                gc.put("type", "Young GC");
                youngGcName = name;
            } else {
                gc.put("type", "Old GC (Full GC)");
                oldGcName = name;
            }

            collectors.add(gc);
            totalGcCount += gcBean.getCollectionCount();
            totalGcTime += gcBean.getCollectionTime();
        }

        gcInfo.put("collectors", collectors);
        gcInfo.put("total_gc_count", totalGcCount);
        gcInfo.put("total_gc_time_ms", totalGcTime);
        gcInfo.put("young_gc_collector", youngGcName);
        gcInfo.put("old_gc_collector", oldGcName);

        // 识别 GC 组合
        String gcCombination = identifyGcCombination(youngGcName, oldGcName);
        gcInfo.put("gc_strategy", gcCombination);

        // 记录增量
        long gcCountDelta = totalGcCount - lastGcCount;
        long gcTimeDelta = totalGcTime - lastGcTime;
        gcInfo.put("gc_count_delta", gcCountDelta);
        gcInfo.put("gc_time_delta_ms", gcTimeDelta);

        lastGcCount = totalGcCount;
        lastGcTime = totalGcTime;

        // 记录到历史
        recordGcHistory(totalGcCount, totalGcTime);

        return gcInfo;
    }

    /**
     * 识别 GC 组合策略
     */
    private String identifyGcCombination(String young, String old) {
        String lowerYoung = young.toLowerCase();
        String lowerOld = old.toLowerCase();

        if (lowerYoung.contains("g1") || lowerOld.contains("g1")) {
            return "G1 GC (推荐用于大堆内存)";
        } else if (lowerYoung.contains("zgc") || lowerOld.contains("zgc")) {
            return "ZGC (低延迟，JDK 11+)";
        } else if (lowerYoung.contains("shenandoah")) {
            return "Shenandoah (低延迟)";
        } else if (lowerYoung.contains("parnew") && lowerOld.contains("cms")) {
            return "ParNew + CMS (已废弃)";
        } else if (lowerYoung.contains("parallel")) {
            return "Parallel GC (吞吐量优先)";
        } else if (lowerYoung.contains("copy") && lowerOld.contains("marksweepcompact")) {
            return "Serial GC (单线程，适合小堆)";
        }
        return young + " + " + old;
    }

    /**
     * 分析内存池
     */
    private Map<String, Object> analyzeMemoryPools() {
        Map<String, Object> pools = new LinkedHashMap<>();

        for (MemoryPoolMXBean poolBean : ManagementFactory.getMemoryPoolMXBeans()) {
            Map<String, Object> pool = new LinkedHashMap<>();
            MemoryUsage usage = poolBean.getUsage();

            pool.put("type", poolBean.getType().toString());
            pool.put("used_mb", usage.getUsed() / 1024 / 1024);
            pool.put("committed_mb", usage.getCommitted() / 1024 / 1024);

            if (usage.getMax() != -1) {
                pool.put("max_mb", usage.getMax() / 1024 / 1024);
                double usagePercent = (double) usage.getUsed() / usage.getMax() * 100;
                pool.put("usage_percent", String.format("%.2f%%", usagePercent));

                if (usagePercent > 90) {
                    pool.put("warning", "⚠️ 使用率超过 90%");
                }
            }

            pools.put(poolBean.getName(), pool);
        }

        return pools;
    }

    /**
     * 计算 GC 效率指标
     */
    private Map<String, Object> calculateGcEfficiency() {
        Map<String, Object> efficiency = new LinkedHashMap<>();

        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        long uptime = runtimeMXBean.getUptime();

        long totalGcTime = 0;
        long youngGcTime = 0;
        long oldGcTime = 0;
        long youngGcCount = 0;
        long oldGcCount = 0;

        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            String name = gcBean.getName().toLowerCase();
            if (name.contains("young") || name.contains("minor") ||
                    name.contains("parnew") || name.contains("copy") ||
                    name.contains("scavenge") || name.contains("g1 young")) {
                youngGcTime += gcBean.getCollectionTime();
                youngGcCount += gcBean.getCollectionCount();
            } else {
                oldGcTime += gcBean.getCollectionTime();
                oldGcCount += gcBean.getCollectionCount();
            }
            totalGcTime += gcBean.getCollectionTime();
        }

        // GC 时间占比
        double gcTimePercent = uptime > 0 ? (double) totalGcTime / uptime * 100 : 0;
        efficiency.put("gc_time_percent", String.format("%.4f%%", gcTimePercent));
        efficiency.put("uptime_seconds", uptime / 1000);

        // Young GC 指标
        efficiency.put("young_gc_count", youngGcCount);
        efficiency.put("young_gc_total_time_ms", youngGcTime);
        if (youngGcCount > 0) {
            efficiency.put("young_gc_avg_time_ms",
                    String.format("%.2f", (double) youngGcTime / youngGcCount));
        }

        // Old GC 指标
        efficiency.put("old_gc_count", oldGcCount);
        efficiency.put("old_gc_total_time_ms", oldGcTime);
        if (oldGcCount > 0) {
            efficiency.put("old_gc_avg_time_ms",
                    String.format("%.2f", (double) oldGcTime / oldGcCount));
        }

        // 健康评估
        if (gcTimePercent < 1) {
            efficiency.put("health", "✅ 优秀 (GC 时间 < 1%)");
        } else if (gcTimePercent < 5) {
            efficiency.put("health", "⚠️ 一般 (GC 时间 1-5%)");
        } else {
            efficiency.put("health", "❌ 需优化 (GC 时间 > 5%)");
        }

        return efficiency;
    }

    /**
     * 生成优化建议
     */
    private List<String> generateOptimizationRecommendations() {
        List<String> recommendations = new ArrayList<>();

        // 获取内存信息
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
        double heapUsagePercent = (double) heapUsage.getUsed() / heapUsage.getMax() * 100;

        // 堆内存使用建议
        if (heapUsagePercent > 80) {
            recommendations.add("💡 堆内存使用率较高 (" + String.format("%.1f", heapUsagePercent) +
                    "%)，建议增加 -Xmx 或检查内存泄漏");
        }

        // GC 策略建议
        long maxHeapMb = heapUsage.getMax() / 1024 / 1024;
        if (maxHeapMb > 4096) {
            recommendations.add("💡 大堆内存(>" + maxHeapMb + "MB)，建议使用 G1 GC: -XX:+UseG1GC");
        }

        // Old GC 频率检查
        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            String name = gcBean.getName().toLowerCase();
            if (!name.contains("young") && !name.contains("minor") && !name.contains("copy")) {
                if (gcBean.getCollectionCount() > 10) {
                    recommendations.add("💡 Full GC 次数较多(" + gcBean.getCollectionCount() +
                            "次)，检查老年代大小或晋升阈值");
                }
            }
        }

        // 常规建议
        if (recommendations.isEmpty()) {
            recommendations.add("✅ GC 状态良好，暂无优化建议");
        }

        return recommendations;
    }

    /**
     * 记录 GC 历史
     */
    private void recordGcHistory(long gcCount, long gcTime) {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("timestamp", System.currentTimeMillis());
        record.put("gc_count", gcCount);
        record.put("gc_time_ms", gcTime);

        synchronized (gcHistory) {
            gcHistory.add(0, record);
            if (gcHistory.size() > MAX_HISTORY_SIZE) {
                gcHistory.remove(gcHistory.size() - 1);
            }
        }
    }

    /**
     * 获取 GC 趋势
     */
    private Map<String, Object> getGcTrend() {
        Map<String, Object> trend = new LinkedHashMap<>();
        trend.put("data_points", Math.min(gcHistory.size(), 10));
        trend.put("recent_records", gcHistory.size() > 10 ? gcHistory.subList(0, 10) : gcHistory);
        return trend;
    }
}
