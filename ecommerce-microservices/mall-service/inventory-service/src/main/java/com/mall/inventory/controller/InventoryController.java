package com.mall.inventory.controller;

import com.mall.common.result.Result;
import com.mall.inventory.entity.Inventory;
import com.mall.inventory.service.InventoryService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/{productId}")
    public Result<Inventory> findByProductId(@PathVariable Long productId) {
        return Result.success(inventoryService.findByProductId(productId));
    }

    @GetMapping("/{productId}/stock")
    public Result<Integer> getStock(@PathVariable Long productId) {
        return Result.success(inventoryService.getStock(productId));
    }

    @PostMapping("/init")
    public Result<Inventory> initStock(@RequestParam Long productId, @RequestParam Integer stock) {
        return Result.success(inventoryService.initStock(productId, stock));
    }

    @PostMapping("/{productId}/deduct")
    public Result<Boolean> deduct(@PathVariable Long productId, @RequestParam Integer quantity) {
        return Result.success(inventoryService.deduct(productId, quantity));
    }

    @PostMapping("/{productId}/restore")
    public Result<Void> restore(@PathVariable Long productId, @RequestParam Integer quantity) {
        inventoryService.restore(productId, quantity);
        return Result.success();
    }

    @PutMapping("/{productId}")
    public Result<Inventory> updateStock(@PathVariable Long productId, @RequestParam Integer stock) {
        return Result.success(inventoryService.updateStock(productId, stock));
    }
}
