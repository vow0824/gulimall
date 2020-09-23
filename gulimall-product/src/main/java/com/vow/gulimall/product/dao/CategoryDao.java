package com.vow.gulimall.product.dao;

import com.vow.gulimall.product.entity.CategoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品三级分类
 * 
 * @author wushaopeng
 * @email wushaopeng@gmail.com
 * @date 2020-09-16 10:21:10
 */
@Mapper
public interface CategoryDao extends BaseMapper<CategoryEntity> {
	
}
