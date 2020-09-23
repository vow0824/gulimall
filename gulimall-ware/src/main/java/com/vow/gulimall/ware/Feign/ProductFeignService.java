package com.vow.gulimall.ware.Feign;

import com.vow.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient("gulimall-product")
public interface ProductFeignService {

    /**
     * /product/skuinfo/info/{skuId}
     * /api/product/skuinfo/info/{skuId}
     *
     * 1、让所有请求过网关
     *  1）、@FeignClient("gulimall-gateway")：给gulimall-product所在的服务器发请求
     *  2）、/api/product/skuinfo/info/{skuId}
     *
     * 2、直接让后台指定服务处理
     *  1）、@FeignClient("gulimall-product")
     *  2）、/product/skuinfo/info/{skuId}
     * @param skuId
     * @return
     */
    @RequestMapping("/product/skuinfo/info/{skuId}")
    public R info(@PathVariable("skuId") Long skuId);
}
