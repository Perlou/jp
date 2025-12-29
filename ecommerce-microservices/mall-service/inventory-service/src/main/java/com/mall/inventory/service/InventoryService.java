package com.mall.inventory.service;

import com.mall.common.exception.BusinessException;
import com.mall.inventory.entity.Inventory;
import com.mall.inventory.repository.InventoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);
    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    public Inventory findByProductId(Long productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> BusinessException.notFound("库存信息不存在"));
    }

    public Integer getStock(Long productId) {
        return findByProductId(productId).getAvailableStock();
    }

    @Transactional
    public Inventory initStock(Long productId, Integer stock) {
        if (inventoryRepository.findByProductId(productId).isPresent()) {
            throw BusinessException.of("库存已存在");
        }
        Inventory inventory = new Inventory();
        inventory.setProductId(productId);
        inventory.setStock(stock);
        return inventoryRepository.save(inventory);
    }

    @Transactional
    public boolean deduct(Long productId, Integer quantity) {
        int updated = inventoryRepository.deductStock(productId, quantity);
        if (updated == 0) {
            log.warn("库存扣减失败: productId={}, quantity={}", productId, quantity);
            throw BusinessException.of("库存不足");
        }
        log.info("库存扣减成功: productId={}, quantity={}", productId, quantity);
        return true;
    }

    @Transactional
    public void restore(Long productId, Integer quantity) {
        inventoryRepository.restoreStock(productId, quantity);
        log.info("库存恢复: productId={}, quantity={}", productId, quantity);
    }

    @Transactional
    public Inventory updateStock(Long productId, Integer stock) {
        Inventory inventory = findByProductId(productId);
        inventory.setStock(stock);
        return inventoryRepository.save(inventory);
    }
}
