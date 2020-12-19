package com.vow.gulimall.ware.vo;

import lombok.Data;

import java.util.List;

@Data
public class SkuWareHasStockVo {

    private Long SkuId;

    private Integer num;

    private List<Long> wareId;

}
