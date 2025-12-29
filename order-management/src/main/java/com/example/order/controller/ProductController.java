package com.example.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.order.common.Result;
import com.example.order.entity.Product;
import com.example.order.service.ProductService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品控制器
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public Result<Product> create(@RequestBody Product product) {
        return Result.success(productService.create(product));
    }

    @GetMapping("/{id}")
    public Result<Product> findById(@PathVariable Long id) {
        return Result.success(productService.findById(id));
    }

    @GetMapping
    public Result<List<Product>> findAll() {
        return Result.success(productService.findAll());
    }

    @GetMapping("/page")
    public Result<Page<Product>> findPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long categoryId) {
        return Result.success(productService.findPage(pageNum, pageSize, categoryId));
    }

    @GetMapping("/search")
    public Result<List<Product>> search(@RequestParam String keyword) {
        return Result.success(productService.search(keyword));
    }

    @PutMapping("/{id}")
    public Result<Product> update(@PathVariable Long id, @RequestBody Product product) {
        return Result.success(productService.update(id, product));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return Result.success();
    }
}
