package com.vow.gulimall.order.dao;

import com.vow.gulimall.order.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 订单
 * 
 * @author wushaopeng
 * @email wushaopeng@gmail.com
 * @date 2020-09-16 14:01:29
 */
@Mapper
public interface OrderDao extends BaseMapper<OrderEntity> {

    void updateOrderStatus(@Param("out_trade_no") String out_trade_no, @Param("code") Integer code);
}
