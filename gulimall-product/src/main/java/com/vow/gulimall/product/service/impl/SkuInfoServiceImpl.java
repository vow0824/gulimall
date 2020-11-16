package com.vow.gulimall.product.service.impl;

import com.vow.gulimall.product.entity.SkuImagesEntity;
import com.vow.gulimall.product.entity.SpuInfoDescEntity;
import com.vow.gulimall.product.service.AttrGroupService;
import com.vow.gulimall.product.service.SkuImagesService;
import com.vow.gulimall.product.service.SpuInfoDescService;
import com.vow.gulimall.product.vo.SkuItemVo;
import com.vow.gulimall.product.vo.SpuItemAttrGroupVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vow.common.utils.PageUtils;
import com.vow.common.utils.Query;

import com.vow.gulimall.product.dao.SkuInfoDao;
import com.vow.gulimall.product.entity.SkuInfoEntity;
import com.vow.gulimall.product.service.SkuInfoService;
import org.springframework.util.StringUtils;


@Service("skuInfoService")
public class SkuInfoServiceImpl extends ServiceImpl<SkuInfoDao, SkuInfoEntity> implements SkuInfoService {

    @Autowired
    SkuImagesService skuImagesService;

    @Autowired
    SpuInfoDescService spuInfoDescService;

    @Autowired
    AttrGroupService attrGroupService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                new QueryWrapper<SkuInfoEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void saveSkuInfo(SkuInfoEntity skuInfoEntity) {
        this.baseMapper.insert(skuInfoEntity);
    }

    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {
        QueryWrapper<SkuInfoEntity> queryWrapper = new QueryWrapper<>();
        String key = (String) params.get("key");
        if (!StringUtils.isEmpty(key)){
            queryWrapper.and(w -> {
                w.eq("sku_id", key).or().like("sku_name", key);
            });
        }
        String catelogId = (String) params.get("catelogId");
        if (!StringUtils.isEmpty(key) && !"0".equalsIgnoreCase(catelogId)){
            queryWrapper.eq("catalog_id", catelogId);
        }
        String brandId = (String) params.get("brandId");
        if (!StringUtils.isEmpty(key) && !"0".equalsIgnoreCase(brandId)){
            queryWrapper.eq("brand_id", brandId);
        }
        String min = (String) params.get("min");
        if (!StringUtils.isEmpty(key)){
            queryWrapper.ge("price", min);
        }
        String max = (String) params.get("max");
        if (!StringUtils.isEmpty(key)){
            try{
                BigDecimal bigMax = new BigDecimal(max);
                if (bigMax.compareTo(new BigDecimal("0")) == 1) {
                    queryWrapper.le("price", max);
                }
            }catch (Exception e){

            }
        }

        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                queryWrapper
        );

        return new PageUtils(page);
    }

    @Override
    public List<SkuInfoEntity> getSkusBySpuId(Long spuId) {

        List<SkuInfoEntity> list = this.list(new QueryWrapper<SkuInfoEntity>().eq("spu_id", spuId));
        return list;
    }

    @Override
    public SkuItemVo item(Long skuId) {
        SkuItemVo skuItemVo = new SkuItemVo();
        // 1、查询sku的基本信息 pms_sku_info
        SkuInfoEntity info = getById(skuId);
        skuItemVo.setInfo(info);
        Long catalogId = info.getCatalogId();
        Long spuId = info.getSpuId();

        // 2、查询sku的图品信息 pms_sku_images
        List<SkuImagesEntity> images =  skuImagesService.getImagesBySkuId(skuId);
        skuItemVo.setImages(images);

        // 3、获取spu的销售属性组合

        // 4、查询spu的介绍
        SpuInfoDescEntity spuInfo = spuInfoDescService.getById(spuId);
        skuItemVo.setDesp(spuInfo);

        // 5、查询spu的规格参数信息
        List<SpuItemAttrGroupVo> attrGroupVos = attrGroupService.getAttrGroupWithAttrsBySpuId(spuId, catalogId);
        skuItemVo.setGroupattrs(attrGroupVos);

        return skuItemVo;
    }

}