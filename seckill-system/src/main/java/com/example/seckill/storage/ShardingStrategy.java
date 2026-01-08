package com.example.seckill.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * 数据分片策略
 * 分布式存储架构 - 数据分布策略
 * 
 * 策略对比:
 * 1. 哈希分片: hash(key) % N，均匀但扩容需迁移大量数据
 * 2. 一致性哈希: 虚拟环形空间，扩容只迁移相邻节点数据
 * 3. 范围分片: 按键范围划分，范围查询高效但可能不均匀
 */
@Component
public class ShardingStrategy {

    private static final Logger log = LoggerFactory.getLogger(ShardingStrategy.class);

    // 一致性哈希环 (使用跳表实现)
    private final NavigableMap<Long, String> hashRing = new ConcurrentSkipListMap<>();

    // 虚拟节点数量
    private static final int VIRTUAL_NODES = 150;

    // 模拟的物理节点列表
    private final List<String> physicalNodes = Arrays.asList(
            "node-1", "node-2", "node-3", "node-4");

    // 范围分片配置
    private final TreeMap<Long, String> rangeShards = new TreeMap<>();

    public ShardingStrategy() {
        initConsistentHash();
        initRangeSharding();
        log.info("分片策略初始化完成 - 物理节点: {}", physicalNodes);
    }

    // ========== 一致性哈希分片 ==========

    /**
     * 初始化一致性哈希环
     */
    private void initConsistentHash() {
        for (String node : physicalNodes) {
            addNode(node);
        }
        log.info("一致性哈希环初始化完成，总虚拟节点数: {}", hashRing.size());
    }

    /**
     * 添加节点到哈希环
     */
    public void addNode(String node) {
        for (int i = 0; i < VIRTUAL_NODES; i++) {
            long hash = hash(node + "-vn-" + i);
            hashRing.put(hash, node);
        }
        log.info("节点 {} 已添加到哈希环 ({} 个虚拟节点)", node, VIRTUAL_NODES);
    }

    /**
     * 从哈希环移除节点
     */
    public void removeNode(String node) {
        for (int i = 0; i < VIRTUAL_NODES; i++) {
            long hash = hash(node + "-vn-" + i);
            hashRing.remove(hash);
        }
        log.info("节点 {} 已从哈希环移除", node);
    }

    /**
     * 使用一致性哈希获取数据应存储的节点
     */
    public String getNodeByConsistentHash(String key) {
        if (hashRing.isEmpty()) {
            return null;
        }

        long hash = hash(key);

        // 顺时针查找第一个节点
        Map.Entry<Long, String> entry = hashRing.ceilingEntry(hash);
        if (entry == null) {
            // 如果没有，则取第一个节点（环形结构）
            entry = hashRing.firstEntry();
        }

        String node = entry.getValue();
        log.debug("[一致性哈希] key={}, hash={}, node={}", key, hash, node);
        return node;
    }

    /**
     * 获取一致性哈希的详细信息
     */
    public Map<String, Object> getConsistentHashInfo(String key) {
        Map<String, Object> info = new LinkedHashMap<>();

        long keyHash = hash(key);
        String targetNode = getNodeByConsistentHash(key);

        info.put("key", key);
        info.put("keyHash", keyHash);
        info.put("targetNode", targetNode);
        info.put("totalVirtualNodes", hashRing.size());
        info.put("physicalNodes", physicalNodes);

        // 统计每个物理节点的虚拟节点分布
        Map<String, Integer> distribution = new LinkedHashMap<>();
        for (String node : physicalNodes) {
            distribution.put(node, 0);
        }
        for (String node : hashRing.values()) {
            distribution.merge(node, 1, Integer::sum);
        }
        info.put("virtualNodeDistribution", distribution);

        // 计算扩容影响
        info.put("scalingBenefit", "添加新节点只影响相邻节点约 1/N 的数据");

        return info;
    }

    // ========== 范围分片 ==========

    /**
     * 初始化范围分片
     */
    private void initRangeSharding() {
        // 按订单ID范围分片
        rangeShards.put(0L, "shard-orders-0"); // 0 - 99999
        rangeShards.put(100000L, "shard-orders-1"); // 100000 - 199999
        rangeShards.put(200000L, "shard-orders-2"); // 200000 - 299999
        rangeShards.put(300000L, "shard-orders-3"); // 300000+
    }

    /**
     * 使用范围分片获取数据应存储的分片
     */
    public String getShardByRange(Long id) {
        Map.Entry<Long, String> entry = rangeShards.floorEntry(id);
        String shard = entry != null ? entry.getValue() : rangeShards.firstEntry().getValue();
        log.debug("[范围分片] id={}, shard={}", id, shard);
        return shard;
    }

    /**
     * 获取范围分片的详细信息
     */
    public Map<String, Object> getRangeShardingInfo(Long id) {
        Map<String, Object> info = new LinkedHashMap<>();

        String shard = getShardByRange(id);

        info.put("id", id);
        info.put("targetShard", shard);
        info.put("shardRanges", rangeShards);

        // 范围分片的优缺点
        info.put("advantages", List.of(
                "范围查询高效",
                "适合时间序列数据",
                "易于理解和管理"));
        info.put("disadvantages", List.of(
                "可能导致数据不均匀",
                "热点问题（新数据集中在一个分片）",
                "扩容需要重新划分范围"));

        return info;
    }

    // ========== 哈希取模分片 ==========

    /**
     * 简单哈希取模分片
     */
    public String getShardByHashMod(String key, int shardCount) {
        int hash = Math.abs(key.hashCode());
        int shardIndex = hash % shardCount;
        String shard = "shard-" + shardIndex;
        log.debug("[哈希取模] key={}, hash={}, shardCount={}, shard={}",
                key, hash, shardCount, shard);
        return shard;
    }

    /**
     * 获取哈希取模分片的详细信息
     */
    public Map<String, Object> getHashModInfo(String key, int shardCount) {
        Map<String, Object> info = new LinkedHashMap<>();

        int hash = Math.abs(key.hashCode());
        int shardIndex = hash % shardCount;

        info.put("key", key);
        info.put("hashValue", hash);
        info.put("shardCount", shardCount);
        info.put("shardIndex", shardIndex);
        info.put("targetShard", "shard-" + shardIndex);

        // 扩容影响分析
        int newShardCount = shardCount + 1;
        int newShardIndex = hash % newShardCount;
        boolean needMigration = shardIndex != newShardIndex;

        info.put("scalingAnalysis", Map.of(
                "currentShardCount", shardCount,
                "newShardCount", newShardCount,
                "newShardIndex", newShardIndex,
                "needMigration", needMigration,
                "migrationRatio", "约 (N-1)/N 的数据需要迁移"));

        return info;
    }

    // ========== 分片策略对比 ==========

    /**
     * 获取分片策略对比信息
     */
    public Map<String, Object> getStrategiesComparison() {
        return Map.of(
                "hashMod", Map.of(
                        "name", "哈希取模分片",
                        "formula", "hash(key) % N",
                        "pros", List.of("均匀分布", "实现简单"),
                        "cons", List.of("扩容需迁移大量数据", "不支持范围查询"),
                        "useCase", "用户ID分片、Session分片"),
                "consistentHash", Map.of(
                        "name", "一致性哈希分片",
                        "formula", "顺时针查找虚拟节点",
                        "pros", List.of("扩容只迁移 1/N 数据", "节点增减影响小"),
                        "cons", List.of("实现复杂", "虚拟节点内存开销"),
                        "useCase", "缓存集群、分布式存储"),
                "range", Map.of(
                        "name", "范围分片",
                        "formula", "按键范围划分",
                        "pros", List.of("范围查询高效", "适合时间序列"),
                        "cons", List.of("可能不均匀", "热点问题"),
                        "useCase", "订单表、日志表"),
                "directory", Map.of(
                        "name", "目录分片",
                        "formula", "查表确定位置",
                        "pros", List.of("灵活", "支持任意规则"),
                        "cons", List.of("目录成为瓶颈", "单点故障风险"),
                        "useCase", "复杂业务规则分片"));
    }

    // ========== 工具方法 ==========

    /**
     * 计算哈希值 (使用 MD5)
     */
    private long hash(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(key.getBytes(StandardCharsets.UTF_8));
            // 取前 8 字节转为 long
            long hash = 0;
            for (int i = 0; i < 8; i++) {
                hash = (hash << 8) | (digest[i] & 0xff);
            }
            return hash;
        } catch (NoSuchAlgorithmException e) {
            // Fallback to simple hash
            return key.hashCode();
        }
    }
}
