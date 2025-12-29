package com.mall.order.feign;

import com.mall.common.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class InventoryClientFallback implements InventoryClient {

    private static final Logger log = LoggerFactory.getLogger(InventoryClientFallback.class);

    @Override
    public Result<Boolean> deduct(Long productId, Integer quantity) {
        log.warn("库存服务调用失败，执行降级: productId={}, quantity={}", productId, quantity);
        return Result.fail("库存服务暂不可用");
    }

    @Override
    public Result<Void> restore(Long productId, Integer quantity) {
        log.warn("库存恢复失败，执行降级: productId={}, quantity={}", productId, quantity);
        return Result.fail("库存服务暂不可用");
    }
}
