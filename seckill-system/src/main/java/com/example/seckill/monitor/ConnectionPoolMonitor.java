package com.example.seckill.monitor;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.*;

/**
 * 连接池监控服务
 * 
 * HikariCP 连接池监控：
 * - 连接池状态
 * - 活跃/空闲/等待连接数
 * 
 * 连接池调优要点：
 * 1. 最佳连接数 ≈ (CPU核心数 × 2) + 有效磁盘数
 * 2. 避免连接泄漏：确保连接被正确释放
 * 3. 合理设置超时时间
 */
@Service
public class ConnectionPoolMonitor {

    private final DataSource dataSource;

    public ConnectionPoolMonitor(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 获取 HikariCP 连接池状态
     */
    public Map<String, Object> getPoolStatus() {
        Map<String, Object> status = new LinkedHashMap<>();

        if (!(dataSource instanceof HikariDataSource)) {
            status.put("error", "DataSource 不是 HikariDataSource");
            return status;
        }

        HikariDataSource hikariDS = (HikariDataSource) dataSource;
        HikariPoolMXBean poolMXBean = hikariDS.getHikariPoolMXBean();

        if (poolMXBean == null) {
            status.put("error", "连接池尚未初始化");
            return status;
        }

        // 配置信息
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("pool_name", hikariDS.getPoolName());
        config.put("maximum_pool_size", hikariDS.getMaximumPoolSize());
        config.put("minimum_idle", hikariDS.getMinimumIdle());
        config.put("connection_timeout_ms", hikariDS.getConnectionTimeout());
        config.put("idle_timeout_ms", hikariDS.getIdleTimeout());
        config.put("max_lifetime_ms", hikariDS.getMaxLifetime());
        status.put("config", config);

        // 实时状态
        Map<String, Object> runtime = new LinkedHashMap<>();
        runtime.put("total_connections", poolMXBean.getTotalConnections());
        runtime.put("active_connections", poolMXBean.getActiveConnections());
        runtime.put("idle_connections", poolMXBean.getIdleConnections());
        runtime.put("threads_awaiting_connection", poolMXBean.getThreadsAwaitingConnection());
        status.put("runtime", runtime);

        // 使用率计算
        int maxSize = hikariDS.getMaximumPoolSize();
        int active = poolMXBean.getActiveConnections();
        double usagePercent = (double) active / maxSize * 100;
        status.put("usage_percent", String.format("%.1f%%", usagePercent));

        // 健康检查与建议
        List<String> suggestions = new ArrayList<>();

        if (usagePercent > 80) {
            suggestions.add("⚠️ 连接池使用率超过 80%，考虑增加 maximum-pool-size");
        }
        if (poolMXBean.getThreadsAwaitingConnection() > 0) {
            suggestions.add("⚠️ 有线程正在等待连接，可能存在连接瓶颈");
        }
        if (active == 0 && poolMXBean.getIdleConnections() == 0) {
            suggestions.add("⚠️ 连接池为空，请检查数据库连接配置");
        }
        if (suggestions.isEmpty()) {
            suggestions.add("✅ 连接池状态正常");
        }

        status.put("suggestions", suggestions);

        // 最佳实践说明
        Map<String, String> bestPractices = new LinkedHashMap<>();
        int cpuCores = Runtime.getRuntime().availableProcessors();
        bestPractices.put("recommended_pool_size",
                String.format("(CPU核心数 × 2) + 磁盘数 = (%d × 2) + 1 ≈ %d", cpuCores, cpuCores * 2 + 1));
        bestPractices.put("current_pool_size", String.valueOf(maxSize));
        status.put("best_practices", bestPractices);

        return status;
    }

    /**
     * 获取连接池配置对比
     * 对比当前配置与推荐配置
     */
    public Map<String, Object> getConfigComparison() {
        Map<String, Object> comparison = new LinkedHashMap<>();

        if (!(dataSource instanceof HikariDataSource)) {
            comparison.put("error", "DataSource 不是 HikariDataSource");
            return comparison;
        }

        HikariDataSource hikariDS = (HikariDataSource) dataSource;
        int cpuCores = Runtime.getRuntime().availableProcessors();
        int recommendedPoolSize = cpuCores * 2 + 1;

        // 配置项对比
        List<Map<String, Object>> items = new ArrayList<>();

        items.add(createComparisonItem("maximum-pool-size",
                hikariDS.getMaximumPoolSize(), recommendedPoolSize,
                "推荐值: (CPU核心数 × 2) + 磁盘数"));

        items.add(createComparisonItem("minimum-idle",
                hikariDS.getMinimumIdle(), hikariDS.getMaximumPoolSize(),
                "建议与 maximum-pool-size 相同"));

        items.add(createComparisonItem("connection-timeout (ms)",
                hikariDS.getConnectionTimeout(), 30000L,
                "获取连接等待超时时间"));

        items.add(createComparisonItem("idle-timeout (ms)",
                hikariDS.getIdleTimeout(), 600000L,
                "空闲连接存活时间 (10分钟)"));

        items.add(createComparisonItem("max-lifetime (ms)",
                hikariDS.getMaxLifetime(), 1800000L,
                "连接最大生命周期 (30分钟)"));

        comparison.put("config_items", items);
        comparison.put("cpu_cores", cpuCores);

        return comparison;
    }

    private Map<String, Object> createComparisonItem(String name, Object current, Object recommended, String note) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", name);
        item.put("current", current);
        item.put("recommended", recommended);
        item.put("note", note);

        if (current.equals(recommended)) {
            item.put("status", "✅");
        } else {
            item.put("status", "⚠️");
        }
        return item;
    }
}
