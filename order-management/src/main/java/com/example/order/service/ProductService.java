package com.example.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.order.entity.Product;
import com.example.order.exception.BusinessException;
import com.example.order.mapper.ProductMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 商品服务 - 含 Redis 缓存
 */
@Service
public class ProductService {

    private final ProductMapper productMapper;
    private final CacheService cacheService;

    public ProductService(ProductMapper productMapper, CacheService cacheService) {
        this.productMapper = productMapper;
        this.cacheService = cacheService;
    }

    @Transactional
    public Product create(Product product) {
        product.setStatus(1);
        product.setVersion(0);
        productMapper.insert(product);
        cacheService.cacheProduct(product);
        return product;
    }

    public Product findById(Long id) {
        // 先查缓存
        Product cached = cacheService.getProductCache(id);
        if (cached != null) {
            return cached;
        }

        // 再查数据库
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw BusinessException.notFound("商品不存在");
        }

        // 写入缓存
        cacheService.cacheProduct(product);
        return product;
    }

    public List<Product> findAll() {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, 1);
        return productMapper.selectList(wrapper);
    }

    public Page<Product> findPage(int pageNum, int pageSize, Long categoryId) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, 1);
        if (categoryId != null) {
            wrapper.eq(Product::getCategoryId, categoryId);
        }
        wrapper.orderByDesc(Product::getCreatedAt);
        return productMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    public List<Product> search(String keyword) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, 1)
                .like(Product::getName, keyword);
        return productMapper.selectList(wrapper);
    }

    @Transactional
    public Product update(Long id, Product dto) {
        Product product = findById(id);
        if (dto.getName() != null)
            product.setName(dto.getName());
        if (dto.getDescription() != null)
            product.setDescription(dto.getDescription());
        if (dto.getPrice() != null)
            product.setPrice(dto.getPrice());
        if (dto.getStock() != null)
            product.setStock(dto.getStock());
        if (dto.getCategoryId() != null)
            product.setCategoryId(dto.getCategoryId());
        productMapper.updateById(product);

        // 更新缓存
        cacheService.evictProductCache(id);
        cacheService.cacheProduct(product);
        return product;
    }

    @Transactional
    public void delete(Long id) {
        Product product = findById(id);
        product.setStatus(0); // 软删除
        productMapper.updateById(product);
        cacheService.evictProductCache(id);
    }

    /**
     * 扣减库存 (原子操作)
     */
    @Transactional
    public void deductStock(Long productId, int quantity) {
        int rows = productMapper.deductStock(productId, quantity);
        if (rows == 0) {
            throw BusinessException.of("库存不足");
        }
        // 清除缓存
        cacheService.evictProductCache(productId);
    }

    /**
     * 恢复库存 (订单取消时)
     */
    @Transactional
    public void restoreStock(Long productId, int quantity) {
        productMapper.restoreStock(productId, quantity);
        cacheService.evictProductCache(productId);
    }
}
