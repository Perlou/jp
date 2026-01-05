package com.example.seckill.cache;

/**
 * 缓存策略枚举
 */
public enum CacheStrategy {

    /**
     * Cache-Aside 模式 (旁路缓存)
     * 读：先读缓存，未命中则读数据库并回填缓存
     * 写：先更新数据库，再删除缓存
     */
    CACHE_ASIDE,

    /**
     * Write-Through 模式 (穿透写)
     * 写操作同时更新缓存和数据库
     */
    WRITE_THROUGH,

    /**
     * Write-Behind 模式 (异步写)
     * 先写缓存，异步批量写入数据库
     */
    WRITE_BEHIND,

    /**
     * Read-Through 模式 (穿透读)
     * 缓存层自动负责从数据库加载数据
     */
    READ_THROUGH
}
