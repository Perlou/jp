package com.mall.order.feign;

import com.mall.common.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ProductClientFallback implements ProductClient {

    private static final Logger log = LoggerFactory.getLogger(ProductClientFallback.class);

    @Override
    public Result<ProductDTO> findById(Long id) {
        log.warn("商品服务调用失败，执行降级: productId={}", id);
        return Result.fail("商品服务暂不可用");
    }
}
