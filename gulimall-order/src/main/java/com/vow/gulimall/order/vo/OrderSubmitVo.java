package com.vow.gulimall.order.vo;


import lombok.Data;

import java.math.BigDecimal;

/**
 * 封装订单提交的数据
 */
@Data
public class OrderSubmitVo {

    private Long addrId;    // 收获地址id

    private Integer payType;    // 支付方式

    // 无需提交需要购买的商品，去购物车再获取一次
    // 优惠发票

    private String orderToken;   // 防重令牌

    private BigDecimal payPrice;    // 支付金额

    // 用户相关信息，直接去session中取

    private String note;    // 订单备注
}
