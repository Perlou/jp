package com.example.seckill.service;

import com.example.seckill.common.Result;
import com.example.seckill.common.SeckillException;
import com.example.seckill.entity.SeckillGoods;
import com.example.seckill.entity.SeckillOrder;
import com.example.seckill.mapper.SeckillGoodsMapper;
import com.example.seckill.mapper.SeckillOrderMapper;
import com.example.seckill.mq.SeckillMessageProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 秒杀服务
 */
@Service
public class SeckillService {

    private static final Logger log = LoggerFactory.getLogger(SeckillService.class);

    private final SeckillGoodsMapper goodsMapper;
    private final SeckillOrderMapper orderMapper;
    private final StringRedisTemplate redisTemplate;
    private final SeckillMessageProducer messageProducer;
    private final DefaultRedisScript<Long> seckillScript;

    // 本地内存标记：商品是否售罄（减少 Redis 访问）
    private final Map<Long, Boolean> localSoldOutMap = new ConcurrentHashMap<>();

    public SeckillService(SeckillGoodsMapper goodsMapper,
            SeckillOrderMapper orderMapper,
            StringRedisTemplate redisTemplate,
            SeckillMessageProducer messageProducer) {
        this.goodsMapper = goodsMapper;
        this.orderMapper = orderMapper;
        this.redisTemplate = redisTemplate;
        this.messageProducer = messageProducer;

        // 初始化 Lua 脚本
        this.seckillScript = new DefaultRedisScript<>();
        this.seckillScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/seckill.lua")));
        this.seckillScript.setResultType(Long.class);
    }

    /**
     * 执行秒杀
     * 
     * @param userId  用户ID
     * @param goodsId 商品ID
     * @return 秒杀结果
     */
    public Result<String> doSeckill(Long userId, Long goodsId) {
        log.info("用户 {} 开始秒杀商品 {}", userId, goodsId);

        // 1. 本地内存标记检查（减少 Redis 访问）
        if (Boolean.TRUE.equals(localSoldOutMap.get(goodsId))) {
            log.warn("商品 {} 本地标记已售罄", goodsId);
            return Result.fail("商品已售罄");
        }

        // 2. 执行 Lua 脚本：原子操作检查库存 + 扣减库存 + 记录购买
        String stockKey = "seckill:stock:" + goodsId;
        String boughtKey = "seckill:bought:" + goodsId;

        Long result = redisTemplate.execute(
                seckillScript,
                Arrays.asList(stockKey, boughtKey),
                userId.toString());

        if (result == null) {
            log.error("Lua 脚本执行失败");
            return Result.error("系统繁忙，请稍后重试");
        }

        if (result == 0) {
            // 库存不足，设置本地标记
            localSoldOutMap.put(goodsId, true);
            log.warn("商品 {} 库存不足", goodsId);
            return Result.fail("商品已售罄");
        }

        if (result == -1) {
            log.warn("用户 {} 重复购买商品 {}", userId, goodsId);
            return Result.fail("您已参与过此秒杀活动");
        }

        // 3. 发送消息到 MQ，异步创建订单
        try {
            messageProducer.sendSeckillMessage(userId, goodsId);
            log.info("秒杀消息发送成功: userId={}, goodsId={}", userId, goodsId);
        } catch (Exception e) {
            log.error("发送秒杀消息失败", e);
            // 回滚 Redis 操作
            rollbackRedis(userId, goodsId);
            return Result.error("系统繁忙，请稍后重试");
        }

        return Result.success("秒杀成功，正在排队处理您的订单", null);
    }

    /**
     * 查询秒杀结果
     */
    public Result<Object> getSeckillResult(Long userId, Long goodsId) {
        // 1. 先查 Redis 缓存的结果
        String resultKey = "seckill:result:" + userId + ":" + goodsId;
        String result = redisTemplate.opsForValue().get(resultKey);

        if ("SUCCESS".equals(result)) {
            // 查询订单详情
            SeckillOrder order = orderMapper.selectByUserAndGoods(userId, goodsId);
            if (order != null) {
                return Result.success("秒杀成功", order);
            }
        } else if ("FAIL".equals(result)) {
            return Result.fail("秒杀失败，库存不足或订单创建失败");
        }

        // 2. 检查是否在排队中
        String boughtKey = "seckill:bought:" + goodsId;
        Boolean isMember = redisTemplate.opsForSet().isMember(boughtKey, userId.toString());
        if (Boolean.TRUE.equals(isMember)) {
            return Result.success("排队中，请稍后查询", null);
        }

        return Result.fail("未参与秒杀");
    }

    /**
     * 获取所有秒杀商品
     */
    public List<SeckillGoods> listSeckillGoods() {
        return goodsMapper.selectOngoingGoods();
    }

    /**
     * 获取秒杀商品详情（含实时库存）
     */
    public SeckillGoods getSeckillGoods(Long goodsId) {
        SeckillGoods goods = goodsMapper.selectById(goodsId);
        if (goods == null) {
            throw new SeckillException("商品不存在");
        }

        // 从 Redis 获取实时库存
        String stockKey = "seckill:stock:" + goodsId;
        String stock = redisTemplate.opsForValue().get(stockKey);
        if (stock != null) {
            goods.setStockCount(Integer.parseInt(stock));
        }

        return goods;
    }

    /**
     * 重置秒杀（测试用）
     */
    public void resetSeckill(Long goodsId) {
        // 清除本地标记
        localSoldOutMap.remove(goodsId);

        // 查询商品
        SeckillGoods goods = goodsMapper.selectById(goodsId);
        if (goods == null) {
            return;
        }

        // 重置 Redis 库存
        String stockKey = "seckill:stock:" + goodsId;
        redisTemplate.opsForValue().set(stockKey, String.valueOf(goods.getStockCount()));

        // 清除已购买记录
        String boughtKey = "seckill:bought:" + goodsId;
        redisTemplate.delete(boughtKey);

        log.info("秒杀已重置: goodsId={}", goodsId);
    }

    /**
     * 回滚 Redis 操作
     */
    private void rollbackRedis(Long userId, Long goodsId) {
        String stockKey = "seckill:stock:" + goodsId;
        String boughtKey = "seckill:bought:" + goodsId;

        redisTemplate.opsForValue().increment(stockKey);
        redisTemplate.opsForSet().remove(boughtKey, userId.toString());
    }

    // ========== 管理接口 ==========

    /**
     * 创建秒杀商品
     */
    public SeckillGoods createSeckillGoods(SeckillGoods goods) {
        goods.setStatus(1); // 默认进行中
        goodsMapper.insert(goods);

        // 同步库存到 Redis
        String stockKey = "seckill:stock:" + goods.getId();
        redisTemplate.opsForValue().set(stockKey, String.valueOf(goods.getStockCount()));

        log.info("创建秒杀商品成功: id={}, name={}", goods.getId(), goods.getGoodsName());
        return goods;
    }

    /**
     * 更新秒杀商品
     */
    public SeckillGoods updateSeckillGoods(SeckillGoods goods) {
        goodsMapper.updateById(goods);

        // 同步库存到 Redis
        if (goods.getStockCount() != null) {
            String stockKey = "seckill:stock:" + goods.getId();
            redisTemplate.opsForValue().set(stockKey, String.valueOf(goods.getStockCount()));
        }

        // 清除售罄标记
        localSoldOutMap.remove(goods.getId());

        log.info("更新秒杀商品成功: id={}", goods.getId());
        return goodsMapper.selectById(goods.getId());
    }

    /**
     * 删除秒杀商品
     */
    public void deleteSeckillGoods(Long goodsId) {
        goodsMapper.deleteById(goodsId);

        // 清除 Redis 数据
        String stockKey = "seckill:stock:" + goodsId;
        String boughtKey = "seckill:bought:" + goodsId;
        redisTemplate.delete(stockKey);
        redisTemplate.delete(boughtKey);

        // 清除本地标记
        localSoldOutMap.remove(goodsId);

        log.info("删除秒杀商品成功: id={}", goodsId);
    }

    /**
     * 获取所有秒杀订单
     */
    public List<SeckillOrder> listSeckillOrders() {
        return orderMapper.selectAllOrders();
    }
}
