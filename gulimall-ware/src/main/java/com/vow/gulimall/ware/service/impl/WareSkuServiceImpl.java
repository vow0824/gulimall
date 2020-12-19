package com.vow.gulimall.ware.service.impl;

import com.vow.common.exception.NoStockException;
import com.vow.common.utils.R;
import com.vow.gulimall.ware.Feign.ProductFeignService;
import com.vow.gulimall.ware.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vow.common.utils.PageUtils;
import com.vow.common.utils.Query;

import com.vow.gulimall.ware.dao.WareSkuDao;
import com.vow.gulimall.ware.entity.WareSkuEntity;
import com.vow.gulimall.ware.service.WareSkuService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    @Autowired
    ProductFeignService productFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<WareSkuEntity> queryWrapper = new QueryWrapper<>();
        String skuId = (String) params.get("skuId");
        if (!StringUtils.isEmpty(skuId)) {
            queryWrapper.eq("sku_id", skuId);
        }
        String wareId = (String) params.get("wareId");
        if (!StringUtils.isEmpty(wareId)) {
            queryWrapper.eq("ware_id", wareId);
        }
        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                queryWrapper
        );

        return new PageUtils(page);
    }

    @Override
    public void addStock(Long skuId, Long wareId, Integer skuNum) {
        //1、判断如果还没有这个库存记录新增
        List<WareSkuEntity> wareSkuEntities = this.baseMapper.selectList(new QueryWrapper<WareSkuEntity>().eq("sku_id", skuId).eq("ware_id", wareId));
        if (wareSkuEntities == null || wareSkuEntities.size() == 0) {
            WareSkuEntity skuEntity = new WareSkuEntity();
            skuEntity.setSkuId(skuId);
            skuEntity.setWareId(wareId);
            skuEntity.setStock(skuNum);
            skuEntity.setStockLocked(0);
            // 远程查询skuname,如果失败，整个事务无需回滚
            //1、自己catch异常
            //TODO 第二种方法让事务不回滚
            try {
                R info = productFeignService.info(skuId);
                Map<String , Object> data = (Map<String, Object>) info.get("skuInfo");
                if (info.getCode() == 0) {
                    skuEntity.setSkuName((String) data.get("skuName"));
                }
            } catch (Exception e) {

            }

            this.baseMapper.insert(skuEntity);
        } else {
            this.baseMapper.addStock(skuId, wareId, skuNum);
        }
    }

    @Override
    public List<SkuHasStockVo> getSkusHasStock(List<Long> skuIds) {

        List<SkuHasStockVo> skuHasStockVos = skuIds.stream().map(skuId -> {
            SkuHasStockVo vo = new SkuHasStockVo();
            // 查询当前sku的总库存量
            Long count = this.baseMapper.getSkuStock(skuId);

            vo.setSkuId(skuId);
            vo.setHasStock(count == null ? false : count > 0);
            return vo;
        }).collect(Collectors.toList());
        return skuHasStockVos;
    }

    /**
     * 为某个订单锁定库存
     * rollbackFor = NoStockException.class
     * 默认只要是运行时异常都会回滚
     * @param wareSkuLockVo
     * @return
     */
    @Transactional(rollbackFor = NoStockException.class)
    @Override
    public Boolean orderLockStock(WareSkuLockVo wareSkuLockVo) {

        // 1、按照下单的收货地址，找到一个就近仓库。
        // 1、找到每个商品在哪个仓库都有库存
        List<OrderItemVo> orderItemVos = wareSkuLockVo.getLocks();
        List<SkuWareHasStockVo> collect = orderItemVos.stream().map(item -> {
            SkuWareHasStockVo skuWareHasStockVo = new SkuWareHasStockVo();
            Long skuId = item.getSkuId();
            skuWareHasStockVo.setSkuId(skuId);
            skuWareHasStockVo.setNum(item.getCount());
            // 查询这个商品在哪里有库存
            List<Long> wareIds = this.baseMapper.listWareIdHasSkuStock(skuId);
            skuWareHasStockVo.setWareId(wareIds);
            return skuWareHasStockVo;
        }).collect(Collectors.toList());

        // 2、循环遍历仓库锁定商品库存
        for (SkuWareHasStockVo skuWareHasStockVo : collect) {
            Boolean skuStocked = false;
            Long skuId = skuWareHasStockVo.getSkuId();
            List<Long> wareIds = skuWareHasStockVo.getWareId();
            Integer num = skuWareHasStockVo.getNum();
            if (wareIds == null || wareIds.size() == 0) {
                // 没有任何仓库有这个商品的库存
                throw new NoStockException(skuId);
            }
            for (Long wareId : wareIds) {
                // 成功就返回1，否则就是0
                Long count = this.baseMapper.lockSkuStock(skuId, wareId, num);
                if (count == 1) {
                    //
                    skuStocked = true;
                    break;
                } else {
                    // 当前仓库锁失败，重试下一个仓库
                }
            }
            if (!skuStocked) {
                // 当前商品所有仓库都没有锁住
                throw new NoStockException(skuId);
            }
        }

        // 3、全部锁定成功
        return true;
    }



}