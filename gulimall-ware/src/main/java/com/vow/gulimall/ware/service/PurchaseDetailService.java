package com.vow.gulimall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.vow.common.utils.PageUtils;
import com.vow.gulimall.ware.entity.PurchaseDetailEntity;

import java.util.List;
import java.util.Map;

/**
 * 
 *
 * @author wushaopeng
 * @email wushaopeng@gmail.com
 * @date 2020-09-16 14:06:25
 */
public interface PurchaseDetailService extends IService<PurchaseDetailEntity> {

    PageUtils queryPage(Map<String, Object> params);

    List<PurchaseDetailEntity> listDetailByPurchaseId(Long id);
}

