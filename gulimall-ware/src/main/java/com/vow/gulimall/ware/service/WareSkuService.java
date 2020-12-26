package com.vow.gulimall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.vow.common.to.mq.OrderTo;
import com.vow.common.to.mq.StockLockedTo;
import com.vow.common.utils.PageUtils;
import com.vow.gulimall.ware.entity.WareSkuEntity;
import com.vow.gulimall.ware.vo.LockStockResultVo;
import com.vow.gulimall.ware.vo.SkuHasStockVo;
import com.vow.gulimall.ware.vo.WareSkuLockVo;

import java.util.List;
import java.util.Map;

/**
 * 商品库存
 *
 * @author wushaopeng
 * @email wushaopeng@gmail.com
 * @date 2020-09-16 14:06:25
 */
public interface WareSkuService extends IService<WareSkuEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void addStock(Long skuId, Long wareId, Integer skuNum);

    List<SkuHasStockVo> getSkusHasStock(List<Long> skuIds);

    Boolean orderLockStock(WareSkuLockVo wareSkuLockVo);

    void unlockStock(StockLockedTo stockLockedTo);

    void unlockStock(OrderTo orderTo);
}

