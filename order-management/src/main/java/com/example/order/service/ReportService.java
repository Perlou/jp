package com.example.order.service;

import com.example.order.dto.ProductRankDTO;
import com.example.order.dto.SalesReportDTO;
import com.example.order.mapper.OrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 报表服务 - 结合 Redis 缓存
 */
@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);
    private static final String CACHE_KEY_DAILY_SALES = "report:daily_sales:";
    private static final String CACHE_KEY_TOP_PRODUCTS = "report:top_products:";

    private final OrderMapper orderMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    public ReportService(OrderMapper orderMapper, RedisTemplate<String, Object> redisTemplate) {
        this.orderMapper = orderMapper;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 销售报表 - 按日统计
     */
    @SuppressWarnings("unchecked")
    public List<SalesReportDTO> getDailySalesReport(LocalDate startDate, LocalDate endDate) {
        String cacheKey = CACHE_KEY_DAILY_SALES + startDate + "_" + endDate;

        // 先查缓存
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                log.info("从缓存获取销售报表: {}", cacheKey);
                return (List<SalesReportDTO>) cached;
            }
        } catch (Exception e) {
            log.warn("Redis 缓存读取失败: {}", e.getMessage());
        }

        // 查询数据库
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();
        List<SalesReportDTO> result = orderMapper.dailySalesReport(start, end);

        // 写入缓存 (5分钟过期)
        try {
            redisTemplate.opsForValue().set(cacheKey, result, 5, TimeUnit.MINUTES);
            log.info("销售报表已缓存: {}", cacheKey);
        } catch (Exception e) {
            log.warn("Redis 缓存写入失败: {}", e.getMessage());
        }

        return result;
    }

    /**
     * 热销商品 TOP N
     */
    @SuppressWarnings("unchecked")
    public List<ProductRankDTO> getTopProducts(int days, int limit) {
        LocalDate startDate = LocalDate.now().minusDays(days);
        String cacheKey = CACHE_KEY_TOP_PRODUCTS + days + "_" + limit;

        // 先查缓存
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                log.info("从缓存获取热销商品: {}", cacheKey);
                return (List<ProductRankDTO>) cached;
            }
        } catch (Exception e) {
            log.warn("Redis 缓存读取失败: {}", e.getMessage());
        }

        // 查询数据库
        LocalDateTime start = startDate.atStartOfDay();
        List<ProductRankDTO> result = orderMapper.topProducts(start, limit);

        // 写入缓存 (10分钟过期)
        try {
            redisTemplate.opsForValue().set(cacheKey, result, 10, TimeUnit.MINUTES);
            log.info("热销商品已缓存: {}", cacheKey);
        } catch (Exception e) {
            log.warn("Redis 缓存写入失败: {}", e.getMessage());
        }

        return result;
    }

    /**
     * 清除报表缓存
     */
    public void clearReportCache() {
        try {
            redisTemplate.delete(redisTemplate.keys(CACHE_KEY_DAILY_SALES + "*"));
            redisTemplate.delete(redisTemplate.keys(CACHE_KEY_TOP_PRODUCTS + "*"));
            log.info("报表缓存已清除");
        } catch (Exception e) {
            log.warn("清除缓存失败: {}", e.getMessage());
        }
    }
}
