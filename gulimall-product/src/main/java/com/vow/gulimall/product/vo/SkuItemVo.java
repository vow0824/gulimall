package com.vow.gulimall.product.vo;

import com.vow.gulimall.product.entity.SkuImagesEntity;
import com.vow.gulimall.product.entity.SkuInfoEntity;
import com.vow.gulimall.product.entity.SpuInfoDescEntity;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
public class SkuItemVo {

    // 1、查询sku的基本信息 pms_sku_info
    private SkuInfoEntity info;

    // 是否有货
    private boolean hasStock = true;

    // 2、查询sku的图品信息 pms_sku_images
    private List<SkuImagesEntity> images;

    // 3、获取spu的销售属性组合
    private List<SkuItemSaleAttrVo> saleAttr;

    // 4、查询spu的介绍
    private SpuInfoDescEntity desp;

    // 5、查询spu的规格参数信息
    private List<SpuItemAttrGroupVo> groupattrs;

    // 6、当前商品的秒杀优惠信息
    private SeckillInfoVo seckillInfoVo;
}
