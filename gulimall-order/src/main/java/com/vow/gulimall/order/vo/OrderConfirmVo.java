package com.vow.gulimall.order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单确认页需要用到的数据
 */
@Data
public class OrderConfirmVo {

    // 收货地址
    List<MemberAddressVo> address;

    // 所有选中的购物项
    List<OrderItemVo> items;

    // 发票记录。。

    // 优惠券信息（会员积分）
    Integer integration;

    // 订单总额
    BigDecimal total;

    // 应付价格
    BigDecimal payPrice;
}
