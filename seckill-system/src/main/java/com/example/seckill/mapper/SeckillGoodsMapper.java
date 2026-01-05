package com.example.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.seckill.entity.SeckillGoods;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 秒杀商品 Mapper
 */
@Mapper
public interface SeckillGoodsMapper extends BaseMapper<SeckillGoods> {

    /**
     * 查询所有进行中的秒杀商品
     */
    default List<SeckillGoods> selectOngoingGoods() {
        return selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SeckillGoods>()
                .eq(SeckillGoods::getStatus, 1));
    }

    /**
     * 乐观锁扣减库存
     * 
     * @param goodsId 商品ID
     * @return 影响行数，0表示库存不足
     */
    @Update("UPDATE seckill_goods SET stock_count = stock_count - 1 " +
            "WHERE id = #{goodsId} AND stock_count > 0")
    int deductStock(@Param("goodsId") Long goodsId);

    /**
     * 恢复库存（订单失败时）
     */
    @Update("UPDATE seckill_goods SET stock_count = stock_count + 1 WHERE id = #{goodsId}")
    int restoreStock(@Param("goodsId") Long goodsId);
}
