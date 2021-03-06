package com.vow.gulimall.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.vow.common.utils.PageUtils;
import com.vow.gulimall.coupon.entity.SpuBoundsEntity;

import java.util.Map;

/**
 * 商品spu积分设置
 *
 * @author wushaopeng
 * @email wushaopeng@gmail.com
 * @date 2020-09-16 13:40:12
 */
public interface SpuBoundsService extends IService<SpuBoundsEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

