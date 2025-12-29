package com.mall.order.feign;

import com.mall.common.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class UserClientFallback implements UserClient {

    private static final Logger log = LoggerFactory.getLogger(UserClientFallback.class);

    @Override
    public Result<UserDTO> findById(Long id) {
        log.warn("用户服务调用失败，执行降级: userId={}", id);
        return Result.fail("用户服务暂不可用");
    }
}
