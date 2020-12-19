package com.vow.gulimall.ware.vo;

import lombok.Data;

@Data
public class LockStockResultVo {

    private Long skuId;

    private Integer num;

    private Boolean locked;
}
