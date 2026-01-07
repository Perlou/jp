package com.example.seckill.service;

import com.example.seckill.entity.SeckillGoods;
import com.example.seckill.mapper.SeckillGoodsMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 库存预热
 * 应用启动时将秒杀商品库存加载到 Redis
 */
@Component
public class StockInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(StockInitializer.class);

    private final SeckillGoodsMapper goodsMapper;
    private final StringRedisTemplate redisTemplate;

    public StockInitializer(SeckillGoodsMapper goodsMapper, StringRedisTemplate redisTemplate) {
        this.goodsMapper = goodsMapper;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void run(String... args) {
        log.info("========== 开始预热秒杀库存 ==========");

        try {
            // 查询所有进行中的秒杀商品
            List<SeckillGoods> goodsList = goodsMapper.selectOngoingGoods();

            for (SeckillGoods goods : goodsList) {
                // 库存 key
                String stockKey = "seckill:stock:" + goods.getId();
                // 已购买用户集合 key
                String boughtKey = "seckill:bought:" + goods.getId();

                // 设置库存
                redisTemplate.opsForValue().set(stockKey, String.valueOf(goods.getStockCount()));

                // 清空已购买集合（如果是重启的话）
                redisTemplate.delete(boughtKey);

                log.info("预热商品 [{}]: 库存={}", goods.getGoodsName(), goods.getStockCount());
            }

            log.info("========== 库存预热完成，共 {} 个商品 ==========", goodsList.size());
        } catch (Exception e) {
            log.error("库存预热失败", e);
        }
    }
}
