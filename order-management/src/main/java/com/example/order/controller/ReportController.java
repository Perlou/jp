package com.example.order.controller;

import com.example.order.common.Result;
import com.example.order.dto.ProductRankDTO;
import com.example.order.dto.SalesReportDTO;
import com.example.order.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 报表控制器
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * 销售报表 - 按日统计
     * GET /api/reports/sales?startDate=2024-01-01&endDate=2024-01-31
     */
    @GetMapping("/sales")
    public Result<List<SalesReportDTO>> getDailySalesReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(reportService.getDailySalesReport(startDate, endDate));
    }

    /**
     * 热销商品 TOP N
     * GET /api/reports/top-products?days=30&limit=10
     */
    @GetMapping("/top-products")
    public Result<List<ProductRankDTO>> getTopProducts(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "10") int limit) {
        return Result.success(reportService.getTopProducts(days, limit));
    }

    /**
     * 清除报表缓存
     */
    @DeleteMapping("/cache")
    public Result<Void> clearCache() {
        reportService.clearReportCache();
        return Result.success();
    }
}
