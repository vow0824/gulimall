package com.vow.gulimall.order.vo;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 订单确认页需要用到的数据
 */
// @Data
public class OrderConfirmVo {

    // 收货地址
    @Getter @Setter
    List<MemberAddressVo> address;

    // 所有选中的购物项
    @Getter @Setter
    List<OrderItemVo> items;

    @Getter @Setter
    Map<Long, Boolean> stocks;

    // 发票记录。。

    // 优惠券信息（会员积分）
    @Getter @Setter
    Integer integration;

    // 订单总额
    // BigDecimal total;

    // 应付价格
    // BigDecimal payPrice;

    // 防重令牌
    @Getter @Setter
    String orderToken;

    public Integer getCount() {
        Integer count = 0;
        if (items != null) {
            for (OrderItemVo item : items) {
                count = count += item.getCount();
            }
        }
        return count;
    }

    public BigDecimal getTotal() {
        BigDecimal total = new BigDecimal("0");
        if (items != null) {
            for (OrderItemVo item : items) {
                BigDecimal itemsPrice = item.getPrice().multiply(new BigDecimal(item.getCount().toString()));
                total = total.add(itemsPrice);
            }
        }
        return total;
    }

    public BigDecimal getPayPrice() {
        return getTotal();
    }
}
