package com.mall.product.controller;

import com.mall.common.result.Result;
import com.mall.product.entity.Product;
import com.mall.product.service.ProductService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
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

    @GetMapping("/search")
    public Result<List<Product>> search(@RequestParam String keyword) {
        return Result.success(productService.search(keyword));
    }

    @GetMapping("/category/{category}")
    public Result<List<Product>> findByCategory(@PathVariable String category) {
        return Result.success(productService.findByCategory(category));
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
