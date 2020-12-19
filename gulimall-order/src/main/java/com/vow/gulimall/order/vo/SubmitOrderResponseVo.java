package com.vow.gulimall.order.vo;

import com.vow.gulimall.order.entity.OrderEntity;
import lombok.Data;

@Data
public class SubmitOrderResponseVo {

    private OrderEntity order;

    private Integer code;   // 0成功
}
