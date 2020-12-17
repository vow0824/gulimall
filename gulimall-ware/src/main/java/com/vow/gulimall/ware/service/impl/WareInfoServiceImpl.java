package com.vow.gulimall.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.vow.common.utils.R;
import com.vow.gulimall.ware.Feign.MemberFeignService;
import com.vow.gulimall.ware.vo.FareVo;
import com.vow.gulimall.ware.vo.MemberAddressVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vow.common.utils.PageUtils;
import com.vow.common.utils.Query;

import com.vow.gulimall.ware.dao.WareInfoDao;
import com.vow.gulimall.ware.entity.WareInfoEntity;
import com.vow.gulimall.ware.service.WareInfoService;
import org.springframework.util.StringUtils;


@Service("wareInfoService")
public class WareInfoServiceImpl extends ServiceImpl<WareInfoDao, WareInfoEntity> implements WareInfoService {

    @Autowired
    MemberFeignService memberFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<WareInfoEntity> queryWrapper = new QueryWrapper<WareInfoEntity>();
        String key = (String) params.get("key");
        if(!StringUtils.isEmpty(key)) {
            queryWrapper.eq("id", key)
                    .or().like("name", key)
                    .or().like("address", key)
                    .or().like("areacode", key);
        }
        IPage<WareInfoEntity> page = this.page(
                new Query<WareInfoEntity>().getPage(params),
                queryWrapper
        );

        return new PageUtils(page);
    }

    @Override
    public FareVo getFare(Long addrId) {
        FareVo fareVo = new FareVo();

        R r = memberFeignService.info(addrId);
        MemberAddressVo data = r.getData("memberReceiveAddress", new TypeReference<MemberAddressVo>() {
        });
        if (data != null) {
            String phone = data.getPhone();
            String substring = phone.substring(phone.length() - 1, phone.length());
            BigDecimal fare = new BigDecimal(substring);
            fareVo.setFare(fare);
            fareVo.setAddress(data);
            return fareVo;
        }
        return null;
    }

}