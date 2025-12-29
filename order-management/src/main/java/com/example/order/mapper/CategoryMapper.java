package com.example.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.order.entity.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 分类 Mapper
 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {

    @Select("SELECT * FROM categories WHERE parent_id = #{parentId} ORDER BY sort_order")
    List<Category> findByParentId(Long parentId);
}
