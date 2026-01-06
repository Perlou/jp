package com.example.seckill.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.seckill.common.Result;
import com.example.seckill.entity.Product;
import com.example.seckill.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品接口
 */
@RestController
@RequestMapping("/api/products")
@Tag(name = "商品管理", description = "商品 CRUD")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取商品详情")
    public Result<Product> getProduct(@PathVariable Long id) {
        return Result.success(productService.findById(id));
    }

    @GetMapping
    @Operation(summary = "获取上架商品列表")
    public Result<List<Product>> getOnSaleProducts() {
        return Result.success(productService.findOnSale());
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询商品")
    public Result<Page<Product>> getProductPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(productService.findPage(pageNum, pageSize));
    }

    @PostMapping
    @Operation(summary = "创建商品")
    public Result<Product> createProduct(@RequestBody Product product) {
        return Result.success(productService.create(product));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新商品")
    public Result<Product> updateProduct(@PathVariable Long id,
            @RequestBody Product product) {
        product.setId(id);
        return Result.success(productService.update(product));
    }
}
