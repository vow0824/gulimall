package com.vow.gulimall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.vow.common.utils.PageUtils;
import com.vow.gulimall.ware.entity.WareInfoEntity;
import com.vow.gulimall.ware.vo.FareVo;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 仓库信息
 *
 * @author wushaopeng
 * @email wushaopeng@gmail.com
 * @date 2020-09-16 14:06:25
 */
public interface WareInfoService extends IService<WareInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * 根据用户的收货地址计算运费
     * @param addrId
     * @return
     */
    FareVo getFare(Long addrId);
}

