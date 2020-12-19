package com.vow.gulimall.order.to;

import com.vow.gulimall.order.entity.OrderEntity;
import com.vow.gulimall.order.entity.OrderItemEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderCreateTo {

    private OrderEntity order;

    private List<OrderItemEntity> orderItems;

    private BigDecimal payPrice;    // 订单计算的应付总额

    private BigDecimal fare;    // 运费
}
