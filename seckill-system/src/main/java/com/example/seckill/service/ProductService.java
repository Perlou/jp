package com.example.seckill.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.seckill.common.SeckillException;
import com.example.seckill.entity.Product;
import com.example.seckill.mapper.ProductMapper;
import com.example.seckill.monitor.CacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 商品服务
 */
@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductMapper productMapper;
    private final CacheService cacheService;

    public ProductService(ProductMapper productMapper, CacheService cacheService) {
        this.productMapper = productMapper;
        this.cacheService = cacheService;
    }

    /**
     * 获取商品详情 (带缓存)
     */
    public Product findById(Long id) {
        // 先尝试从缓存获取
        String cacheKey = "product:" + id;
        var cacheResult = cacheService.getWithMultiLevelCache(cacheKey);

        // 如果缓存命中，可以解析返回（这里简化处理，直接查库）
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new SeckillException("商品不存在");
        }
        return product;
    }

    /**
     * 商品列表
     */
    public List<Product> findAll() {
        return productMapper.selectList(null);
    }

    /**
     * 分页查询
     */
    public Page<Product> findPage(int pageNum, int pageSize) {
        return productMapper.selectPage(new Page<>(pageNum, pageSize), null);
    }

    /**
     * 上架商品
     */
    public List<Product> findOnSale() {
        return productMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Product>()
                        .eq("status", Product.STATUS_ON));
    }

    /**
     * 创建商品
     */
    public Product create(Product product) {
        product.setStatus(Product.STATUS_ON);
        product.setVersion(0);
        productMapper.insert(product);
        log.info("商品创建成功: {}", product.getName());
        return product;
    }

    /**
     * 更新商品
     */
    public Product update(Product product) {
        productMapper.updateById(product);
        log.info("商品更新成功: {}", product.getId());
        return product;
    }
}
