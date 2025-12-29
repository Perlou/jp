package com.mall.product.service;

import com.mall.common.exception.BusinessException;
import com.mall.product.entity.Product;
import com.mall.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public Product create(Product product) {
        return productRepository.save(product);
    }

    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("商品不存在"));
    }

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public List<Product> search(String keyword) {
        return productRepository.findByNameContaining(keyword);
    }

    public List<Product> findByCategory(String category) {
        return productRepository.findByCategory(category);
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
        if (dto.getCategory() != null)
            product.setCategory(dto.getCategory());
        if (dto.getImageUrl() != null)
            product.setImageUrl(dto.getImageUrl());
        return productRepository.save(product);
    }

    @Transactional
    public void delete(Long id) {
        Product product = findById(id);
        product.setStatus(Product.ProductStatus.DELETED);
        productRepository.save(product);
    }
}
