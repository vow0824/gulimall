package com.vow.gulimall.coupon.dao;

import com.vow.gulimall.coupon.entity.MemberPriceEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品会员价格
 * 
 * @author wushaopeng
 * @email wushaopeng@gmail.com
 * @date 2020-09-16 13:40:12
 */
@Mapper
public interface MemberPriceDao extends BaseMapper<MemberPriceEntity> {
	
}
