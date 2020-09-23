package com.vow.gulimall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.vow.common.utils.PageUtils;
import com.vow.gulimall.ware.entity.PurchaseEntity;
import com.vow.gulimall.ware.vo.MergeVo;
import com.vow.gulimall.ware.vo.PurchaseDoneVo;

import java.util.List;
import java.util.Map;

/**
 * 采购信息
 *
 * @author wushaopeng
 * @email wushaopeng@gmail.com
 * @date 2020-09-16 14:06:25
 */
public interface PurchaseService extends IService<PurchaseEntity> {

    PageUtils queryPage(Map<String, Object> params);

    PageUtils queryPageUnreceive(Map<String, Object> params);

    void mergePurchase(MergeVo mergeVo);

    void received(List<Long> ids);

    void done(PurchaseDoneVo purchaseDoneVo);
}

