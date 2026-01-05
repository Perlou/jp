-- 秒杀 Lua 脚本
-- 原子操作：检查库存 + 检查重复购买 + 扣减库存 + 记录购买
--
-- KEYS[1]: 库存 key (seckill:stock:{goodsId})
-- KEYS[2]: 已购买用户集合 key (seckill:bought:{goodsId})
-- ARGV[1]: 用户ID
--
-- 返回值：
--   1: 成功
--   0: 库存不足
--  -1: 重复购买

-- 检查库存
local stock = tonumber(redis.call('GET', KEYS[1]))
if stock == nil or stock <= 0 then
    return 0  -- 库存不足
end

-- 检查是否已购买
local bought = redis.call('SISMEMBER', KEYS[2], ARGV[1])
if bought == 1 then
    return -1  -- 重复购买
end

-- 扣减库存
redis.call('DECR', KEYS[1])

-- 记录已购买
redis.call('SADD', KEYS[2], ARGV[1])

return 1  -- 成功
