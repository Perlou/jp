package com.example.seckill.monitor;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 性能报告生成服务
 * 
 * 综合各项监控数据，生成完整的性能分析报告：
 * - 系统概览
 * - 性能瓶颈识别
 * - 优化建议汇总
 */
@Service
public class PerformanceReportService {

    private final JvmMonitorService jvmMonitorService;
    private final CacheService cacheService;
    private final ConnectionPoolMonitor connectionPoolMonitor;
    private final ThreadPoolMonitor threadPoolMonitor;
    private final GcLogAnalyzer gcLogAnalyzer;

    public PerformanceReportService(JvmMonitorService jvmMonitorService,
            CacheService cacheService,
            ConnectionPoolMonitor connectionPoolMonitor,
            ThreadPoolMonitor threadPoolMonitor,
            GcLogAnalyzer gcLogAnalyzer) {
        this.jvmMonitorService = jvmMonitorService;
        this.cacheService = cacheService;
        this.connectionPoolMonitor = connectionPoolMonitor;
        this.threadPoolMonitor = threadPoolMonitor;
        this.gcLogAnalyzer = gcLogAnalyzer;
    }

    /**
     * 生成完整性能报告
     */
    public Map<String, Object> generateFullReport() {
        Map<String, Object> report = new LinkedHashMap<>();

        // 报告元数据
        report.put("report_time", LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        report.put("report_type", "Performance Analysis Report");

        // 1. 系统概览
        report.put("system_overview", generateSystemOverview());

        // 2. 内存分析
        report.put("memory_analysis", generateMemoryAnalysis());

        // 3. GC 分析
        report.put("gc_analysis", gcLogAnalyzer.getGcAnalysisReport());

        // 4. 缓存分析
        report.put("cache_analysis", generateCacheAnalysis());

        // 5. 连接池分析
        report.put("connection_pool_analysis", connectionPoolMonitor.getPoolStatus());

        // 6. 线程池分析
        report.put("thread_pool_analysis", threadPoolMonitor.getAllPoolStatus());

        // 7. 瓶颈识别
        report.put("bottleneck_detection", detectBottlenecks());

        // 8. 综合评分
        report.put("performance_score", calculatePerformanceScore());

        // 9. 优化建议汇总
        report.put("recommendations", collectAllRecommendations());

        return report;
    }

    /**
     * 生成系统概览
     */
    private Map<String, Object> generateSystemOverview() {
        Map<String, Object> overview = new LinkedHashMap<>();

        Map<String, Object> runtimeInfo = jvmMonitorService.getRuntimeInfo();
        overview.put("jvm_name", runtimeInfo.get("jvm_name"));
        overview.put("java_version", runtimeInfo.get("java_version"));
        overview.put("uptime_seconds", runtimeInfo.get("uptime_seconds"));
        overview.put("available_processors", runtimeInfo.get("available_processors"));
        overview.put("system_load", runtimeInfo.get("system_load_average"));

        return overview;
    }

    /**
     * 生成内存分析
     */
    private Map<String, Object> generateMemoryAnalysis() {
        Map<String, Object> analysis = new LinkedHashMap<>();

        Map<String, Object> memoryInfo = jvmMonitorService.getMemoryInfo();
        analysis.put("heap", memoryInfo.get("heap"));
        analysis.put("non_heap", memoryInfo.get("non_heap"));

        // 内存健康状态
        @SuppressWarnings("unchecked")
        Map<String, Object> heap = (Map<String, Object>) memoryInfo.get("heap");
        String usagePercent = (String) heap.get("usage_percent");
        double usage = Double.parseDouble(usagePercent.replace("%", ""));

        if (usage < 60) {
            analysis.put("health", "✅ 良好");
        } else if (usage < 80) {
            analysis.put("health", "⚠️ 偏高，需关注");
        } else {
            analysis.put("health", "❌ 危险，需立即处理");
        }

        return analysis;
    }

    /**
     * 生成缓存分析
     */
    private Map<String, Object> generateCacheAnalysis() {
        Map<String, Object> analysis = new LinkedHashMap<>();

        Map<String, Object> cacheStats = cacheService.getCacheStats();
        analysis.put("stats", cacheStats);

        // 缓存效率评估
        String hitRateStr = (String) cacheStats.get("hit_rate");
        if (hitRateStr != null) {
            double hitRate = Double.parseDouble(hitRateStr.replace("%", ""));
            if (hitRate >= 90) {
                analysis.put("efficiency", "✅ 优秀 (命中率 ≥ 90%)");
            } else if (hitRate >= 70) {
                analysis.put("efficiency", "⚠️ 一般 (命中率 70-90%)");
            } else {
                analysis.put("efficiency", "❌ 较差 (命中率 < 70%)");
            }
        }

        return analysis;
    }

    /**
     * 瓶颈检测
     */
    private Map<String, Object> detectBottlenecks() {
        Map<String, Object> bottlenecks = new LinkedHashMap<>();
        List<Map<String, Object>> issues = new ArrayList<>();

        // 检查内存瓶颈
        Map<String, Object> memoryInfo = jvmMonitorService.getMemoryInfo();
        @SuppressWarnings("unchecked")
        Map<String, Object> heap = (Map<String, Object>) memoryInfo.get("heap");
        String usagePercent = (String) heap.get("usage_percent");
        double memUsage = Double.parseDouble(usagePercent.replace("%", ""));
        if (memUsage > 80) {
            Map<String, Object> issue = new LinkedHashMap<>();
            issue.put("type", "MEMORY");
            issue.put("severity", "HIGH");
            issue.put("description", "堆内存使用率过高: " + usagePercent);
            issue.put("suggestion", "增加堆内存或检查内存泄漏");
            issues.add(issue);
        }

        // 检查 GC 瓶颈
        Map<String, Object> gcInfo = jvmMonitorService.getGcInfo();
        long totalGcTime = (Long) gcInfo.get("total_gc_time_ms");
        Map<String, Object> runtimeInfo = jvmMonitorService.getRuntimeInfo();
        long uptime = (Long) runtimeInfo.get("uptime_seconds") * 1000;
        if (uptime > 0 && (double) totalGcTime / uptime > 0.05) {
            Map<String, Object> issue = new LinkedHashMap<>();
            issue.put("type", "GC");
            issue.put("severity", "MEDIUM");
            issue.put("description", "GC 时间占比超过 5%");
            issue.put("suggestion", "优化 GC 配置或减少对象创建");
            issues.add(issue);
        }

        // 检查连接池瓶颈
        Map<String, Object> poolStatus = connectionPoolMonitor.getPoolStatus();
        @SuppressWarnings("unchecked")
        Map<String, Object> runtime = (Map<String, Object>) poolStatus.get("runtime");
        if (runtime != null) {
            Object awaitingObj = runtime.get("threads_awaiting_connection");
            if (awaitingObj != null && Integer.parseInt(String.valueOf(awaitingObj)) > 0) {
                Map<String, Object> issue = new LinkedHashMap<>();
                issue.put("type", "CONNECTION_POOL");
                issue.put("severity", "HIGH");
                issue.put("description", "有线程等待数据库连接");
                issue.put("suggestion", "增加连接池大小或优化 SQL");
                issues.add(issue);
            }
        }

        bottlenecks.put("issues_found", issues.size());
        bottlenecks.put("issues", issues);

        if (issues.isEmpty()) {
            bottlenecks.put("status", "✅ 未检测到明显瓶颈");
        } else {
            bottlenecks.put("status", "⚠️ 检测到 " + issues.size() + " 个潜在瓶颈");
        }

        return bottlenecks;
    }

    /**
     * 计算综合性能评分 (0-100)
     */
    private Map<String, Object> calculatePerformanceScore() {
        Map<String, Object> scoring = new LinkedHashMap<>();
        int totalScore = 0;
        int maxScore = 0;

        // 内存评分 (30分)
        maxScore += 30;
        Map<String, Object> memoryInfo = jvmMonitorService.getMemoryInfo();
        @SuppressWarnings("unchecked")
        Map<String, Object> heap = (Map<String, Object>) memoryInfo.get("heap");
        double memUsage = Double.parseDouble(((String) heap.get("usage_percent")).replace("%", ""));
        int memScore = memUsage < 60 ? 30 : memUsage < 80 ? 20 : memUsage < 90 ? 10 : 0;
        totalScore += memScore;
        scoring.put("memory_score", memScore + "/30");

        // GC 评分 (25分)
        maxScore += 25;
        Map<String, Object> gcInfo = jvmMonitorService.getGcInfo();
        long totalGcTime = (Long) gcInfo.get("total_gc_time_ms");
        Map<String, Object> runtimeInfo = jvmMonitorService.getRuntimeInfo();
        long uptime = (Long) runtimeInfo.get("uptime_seconds") * 1000;
        double gcPercent = uptime > 0 ? (double) totalGcTime / uptime * 100 : 0;
        int gcScore = gcPercent < 1 ? 25 : gcPercent < 3 ? 20 : gcPercent < 5 ? 10 : 0;
        totalScore += gcScore;
        scoring.put("gc_score", gcScore + "/25");

        // 缓存评分 (25分)
        maxScore += 25;
        Map<String, Object> cacheStats = cacheService.getCacheStats();
        String hitRateStr = (String) cacheStats.get("hit_rate");
        int cacheScore = 15; // 默认分数
        if (hitRateStr != null) {
            double hitRate = Double.parseDouble(hitRateStr.replace("%", ""));
            cacheScore = hitRate >= 90 ? 25 : hitRate >= 70 ? 20 : hitRate >= 50 ? 10 : 5;
        }
        totalScore += cacheScore;
        scoring.put("cache_score", cacheScore + "/25");

        // 连接池评分 (20分)
        maxScore += 20;
        Map<String, Object> poolStatus = connectionPoolMonitor.getPoolStatus();
        @SuppressWarnings("unchecked")
        Map<String, Object> runtime = (Map<String, Object>) poolStatus.get("runtime");
        int poolScore = 20;
        if (runtime != null) {
            Object awaitingObj = runtime.get("threads_awaiting_connection");
            if (awaitingObj != null && Integer.parseInt(String.valueOf(awaitingObj)) > 0) {
                poolScore = 5;
            }
        }
        totalScore += poolScore;
        scoring.put("connection_pool_score", poolScore + "/20");

        // 总分
        scoring.put("total_score", totalScore + "/" + maxScore);
        double percentage = (double) totalScore / maxScore * 100;
        scoring.put("percentage", String.format("%.1f%%", percentage));

        // 等级
        String grade;
        if (percentage >= 90) {
            grade = "A (优秀)";
        } else if (percentage >= 80) {
            grade = "B (良好)";
        } else if (percentage >= 60) {
            grade = "C (一般)";
        } else {
            grade = "D (需改进)";
        }
        scoring.put("grade", grade);

        return scoring;
    }

    /**
     * 收集所有优化建议
     */
    private List<String> collectAllRecommendations() {
        List<String> recommendations = new ArrayList<>();

        // GC 建议
        Map<String, Object> gcReport = gcLogAnalyzer.getGcAnalysisReport();
        @SuppressWarnings("unchecked")
        List<String> gcRecs = (List<String>) gcReport.get("recommendations");
        if (gcRecs != null) {
            recommendations.addAll(gcRecs);
        }

        // 连接池建议
        Map<String, Object> poolStatus = connectionPoolMonitor.getPoolStatus();
        @SuppressWarnings("unchecked")
        List<String> poolSuggestions = (List<String>) poolStatus.get("suggestions");
        if (poolSuggestions != null) {
            recommendations.addAll(poolSuggestions);
        }

        // 缓存建议
        Map<String, Object> cacheStats = cacheService.getCacheStats();
        Object suggestion = cacheStats.get("suggestion");
        if (suggestion != null) {
            recommendations.add((String) suggestion);
        }

        // 去重
        return new ArrayList<>(new LinkedHashSet<>(recommendations));
    }
}
