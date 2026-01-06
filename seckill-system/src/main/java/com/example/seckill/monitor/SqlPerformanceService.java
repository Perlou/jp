package com.example.seckill.monitor;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * SQL 性能分析服务
 * 
 * 提供 SQL 性能监控与优化建议：
 * - 慢查询统计
 * - EXPLAIN 执行计划分析
 * - 索引使用建议
 */
@Service
public class SqlPerformanceService {

    private final JdbcTemplate jdbcTemplate;

    // 慢查询阈值 (毫秒)
    private static final long SLOW_QUERY_THRESHOLD_MS = 100;

    // 记录最近的查询统计
    private final List<Map<String, Object>> recentQueries = Collections.synchronizedList(
            new ArrayList<>());
    private static final int MAX_RECENT_QUERIES = 100;

    public SqlPerformanceService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 执行 EXPLAIN 分析
     * 
     * EXPLAIN 关键字段说明：
     * - type: 访问类型 (system > const > eq_ref > ref > range > index > ALL)
     * - key: 实际使用的索引
     * - rows: 预估扫描行数
     * - Extra: 额外信息 (Using index, Using filesort 等)
     */
    public Map<String, Object> explainQuery(String sql) {
        Map<String, Object> result = new LinkedHashMap<>();

        if (sql == null || sql.trim().isEmpty()) {
            result.put("error", "SQL 不能为空");
            return result;
        }

        // 确保是 SELECT 语句
        String trimmedSql = sql.trim().toLowerCase();
        if (!trimmedSql.startsWith("select")) {
            result.put("error", "只支持 SELECT 语句的 EXPLAIN 分析");
            return result;
        }

        try {
            String explainSql = "EXPLAIN " + sql;
            List<Map<String, Object>> explainResult = jdbcTemplate.queryForList(explainSql);

            result.put("sql", sql);
            result.put("explain_result", explainResult);
            result.put("analysis", analyzeExplainResult(explainResult));

        } catch (Exception e) {
            result.put("error", "执行 EXPLAIN 失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 分析 EXPLAIN 结果并提供优化建议
     */
    private Map<String, Object> analyzeExplainResult(List<Map<String, Object>> explainResult) {
        Map<String, Object> analysis = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();

        for (Map<String, Object> row : explainResult) {
            String type = String.valueOf(row.get("type"));
            String key = row.get("key") != null ? String.valueOf(row.get("key")) : null;
            Object rowsObj = row.get("rows");
            long rows = rowsObj != null ? Long.parseLong(String.valueOf(rowsObj)) : 0;
            String extra = row.get("Extra") != null ? String.valueOf(row.get("Extra")) : "";

            // 检查访问类型
            if ("ALL".equals(type)) {
                warnings.add("⚠️ 全表扫描 (type=ALL)，缺少有效索引");
                suggestions.add("为 WHERE 条件列添加索引");
            } else if ("index".equals(type)) {
                warnings.add("⚠️ 索引全扫描 (type=index)");
                suggestions.add("检查是否可以使用更精确的索引");
            }

            // 检查是否使用索引
            if (key == null) {
                warnings.add("⚠️ 没有使用任何索引");
            }

            // 检查扫描行数
            if (rows > 10000) {
                warnings.add("⚠️ 扫描行数较多: " + rows + " 行");
                suggestions.add("考虑添加更精确的查询条件或分页");
            }

            // 检查 Extra 信息
            if (extra.contains("Using filesort")) {
                warnings.add("⚠️ 使用文件排序 (Using filesort)");
                suggestions.add("为 ORDER BY 列添加索引");
            }
            if (extra.contains("Using temporary")) {
                warnings.add("⚠️ 使用临时表 (Using temporary)");
                suggestions.add("优化 GROUP BY 或 DISTINCT 操作");
            }
            if (extra.contains("Using index")) {
                // 这是好的，使用了覆盖索引
                analysis.put("covering_index", "✅ 使用了覆盖索引");
            }
        }

        if (warnings.isEmpty()) {
            analysis.put("status", "✅ SQL 执行计划良好");
        } else {
            analysis.put("status", "⚠️ 发现潜在性能问题");
        }

        analysis.put("warnings", warnings);
        analysis.put("suggestions", suggestions);

        return analysis;
    }

    /**
     * 获取索引建议
     */
    public Map<String, Object> getIndexSuggestions(String tableName) {
        Map<String, Object> result = new LinkedHashMap<>();

        try {
            // 获取表的索引信息
            String showIndexSql = "SHOW INDEX FROM " + tableName;
            List<Map<String, Object>> indexes = jdbcTemplate.queryForList(showIndexSql);

            result.put("table", tableName);
            result.put("existing_indexes", indexes);

            // 分析索引
            List<String> suggestions = new ArrayList<>();
            Set<String> indexedColumns = new HashSet<>();

            for (Map<String, Object> idx : indexes) {
                indexedColumns.add(String.valueOf(idx.get("Column_name")));
            }

            // 获取表结构
            String descSql = "DESC " + tableName;
            List<Map<String, Object>> columns = jdbcTemplate.queryForList(descSql);

            for (Map<String, Object> col : columns) {
                String colName = String.valueOf(col.get("Field"));
                String colType = String.valueOf(col.get("Type"));
                String key = col.get("Key") != null ? String.valueOf(col.get("Key")) : "";

                // 常见需要索引的列
                if (key.isEmpty()) {
                    if (colName.endsWith("_id") || colName.equals("status") ||
                            colName.equals("created_at") || colName.equals("updated_at")) {
                        suggestions.add("建议为 " + colName + " 列添加索引");
                    }
                }
            }

            result.put("suggestions", suggestions.isEmpty() ? Collections.singletonList("✅ 索引配置良好") : suggestions);
            result.put("columns", columns);

        } catch (Exception e) {
            result.put("error", "获取索引信息失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 记录查询执行时间（用于监控）
     */
    public void recordQueryExecution(String sql, long executionTimeMs) {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("sql", sql.length() > 200 ? sql.substring(0, 200) + "..." : sql);
        record.put("execution_time_ms", executionTimeMs);
        record.put("timestamp", System.currentTimeMillis());
        record.put("slow", executionTimeMs > SLOW_QUERY_THRESHOLD_MS);

        synchronized (recentQueries) {
            recentQueries.add(0, record);
            if (recentQueries.size() > MAX_RECENT_QUERIES) {
                recentQueries.remove(recentQueries.size() - 1);
            }
        }
    }

    /**
     * 获取慢查询统计
     */
    public Map<String, Object> getSlowQueryStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        List<Map<String, Object>> slowQueries = new ArrayList<>();
        long totalQueries = 0;
        long slowCount = 0;
        long totalTime = 0;

        synchronized (recentQueries) {
            for (Map<String, Object> query : recentQueries) {
                totalQueries++;
                long time = (Long) query.get("execution_time_ms");
                totalTime += time;

                if ((Boolean) query.get("slow")) {
                    slowCount++;
                    slowQueries.add(query);
                }
            }
        }

        stats.put("total_queries", totalQueries);
        stats.put("slow_queries", slowCount);
        stats.put("slow_query_threshold_ms", SLOW_QUERY_THRESHOLD_MS);
        stats.put("slow_percentage",
                totalQueries > 0 ? String.format("%.2f%%", (double) slowCount / totalQueries * 100) : "N/A");
        stats.put("avg_execution_time_ms",
                totalQueries > 0 ? String.format("%.2f", (double) totalTime / totalQueries) : "N/A");
        stats.put("recent_slow_queries", slowQueries.size() > 10 ? slowQueries.subList(0, 10) : slowQueries);

        return stats;
    }

    /**
     * 获取 MySQL 常用性能相关变量
     */
    public Map<String, Object> getMySqlPerformanceVariables() {
        Map<String, Object> result = new LinkedHashMap<>();

        try {
            // 查询关键性能变量
            String[] variables = {
                    "innodb_buffer_pool_size",
                    "max_connections",
                    "query_cache_size",
                    "slow_query_log",
                    "long_query_time",
                    "innodb_log_file_size"
            };

            Map<String, Object> values = new LinkedHashMap<>();
            for (String var : variables) {
                try {
                    List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                            "SHOW VARIABLES LIKE '" + var + "'");
                    if (!rows.isEmpty()) {
                        values.put(var, rows.get(0).get("Value"));
                    }
                } catch (Exception ignored) {
                }
            }

            result.put("variables", values);

            // 查询状态信息
            Map<String, Object> status = new LinkedHashMap<>();
            String[] statusVars = { "Threads_connected", "Threads_running", "Slow_queries" };
            for (String var : statusVars) {
                try {
                    List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                            "SHOW STATUS LIKE '" + var + "'");
                    if (!rows.isEmpty()) {
                        status.put(var, rows.get(0).get("Value"));
                    }
                } catch (Exception ignored) {
                }
            }
            result.put("status", status);

        } catch (Exception e) {
            result.put("error", "获取 MySQL 变量失败: " + e.getMessage());
        }

        return result;
    }
}
