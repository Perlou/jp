package com.example.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.seckill.entity.SeckillOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 秒杀订单 Mapper
 */
@Mapper
public interface SeckillOrderMapper extends BaseMapper<SeckillOrder> {

    /**
     * 查询用户是否已购买该商品
     */
    default SeckillOrder selectByUserAndGoods(Long userId, Long goodsId) {
        return selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SeckillOrder>()
                .eq(SeckillOrder::getUserId, userId)
                .eq(SeckillOrder::getGoodsId, goodsId));
    }

    /**
     * 更新订单状态
     */
    @Update("UPDATE seckill_order SET status = #{status}, updated_at = NOW() " +
            "WHERE user_id = #{userId} AND goods_id = #{goodsId}")
    int updateStatus(@Param("userId") Long userId,
            @Param("goodsId") Long goodsId,
            @Param("status") Integer status);

    /**
     * 查询所有订单
     */
    default java.util.List<SeckillOrder> selectAllOrders() {
        return selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SeckillOrder>()
                .orderByDesc(SeckillOrder::getCreatedAt));
    }
}
