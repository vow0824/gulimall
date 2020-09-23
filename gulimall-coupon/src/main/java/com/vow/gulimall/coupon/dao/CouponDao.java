package com.vow.gulimall.coupon.dao;

import com.vow.gulimall.coupon.entity.CouponEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券信息
 * 
 * @author wushaopeng
 * @email wushaopeng@gmail.com
 * @date 2020-09-16 13:40:12
 */
@Mapper
public interface CouponDao extends BaseMapper<CouponEntity> {
	
}
