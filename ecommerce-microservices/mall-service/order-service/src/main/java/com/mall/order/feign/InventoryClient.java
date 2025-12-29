package com.mall.order.feign;

import com.mall.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 库存服务 Feign 客户端
 */
@FeignClient(name = "inventory-service", fallback = InventoryClientFallback.class)
public interface InventoryClient {

    @PostMapping("/inventory/{productId}/deduct")
    Result<Boolean> deduct(@PathVariable("productId") Long productId, @RequestParam("quantity") Integer quantity);

    @PostMapping("/inventory/{productId}/restore")
    Result<Void> restore(@PathVariable("productId") Long productId, @RequestParam("quantity") Integer quantity);
}
