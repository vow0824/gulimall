package com.vow.gulimall.order.dao;

import com.vow.gulimall.order.entity.RefundInfoEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 退款信息
 * 
 * @author wushaopeng
 * @email wushaopeng@gmail.com
 * @date 2020-09-16 14:01:29
 */
@Mapper
public interface RefundInfoDao extends BaseMapper<RefundInfoEntity> {
	
}
