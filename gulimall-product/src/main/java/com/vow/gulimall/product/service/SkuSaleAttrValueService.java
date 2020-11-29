package com.vow.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.vow.common.utils.PageUtils;
import com.vow.gulimall.product.entity.SkuSaleAttrValueEntity;
import com.vow.gulimall.product.vo.SkuItemSaleAttrVo;

import java.util.List;
import java.util.Map;

/**
 * sku销售属性&值
 *
 * @author wushaopeng
 * @email wushaopeng@gmail.com
 * @date 2020-09-16 10:21:10
 */
public interface SkuSaleAttrValueService extends IService<SkuSaleAttrValueEntity> {

    PageUtils queryPage(Map<String, Object> params);

    List<SkuItemSaleAttrVo> getSaleAttrsBySpuId(Long spuId);

    List<String> getSkuSaleAttrValuesAsStringList(Long skuId);
}

