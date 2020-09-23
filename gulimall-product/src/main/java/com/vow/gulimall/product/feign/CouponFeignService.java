package com.vow.gulimall.product.feign;

import com.vow.common.to.SkuReductionTo;
import com.vow.common.to.SpuBoundsTo;
import com.vow.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("gulimall-coupon")
public interface CouponFeignService {

    /**
     * 1.CouponFeignService.saveSpuBounds(@RequestBody SpuBoundsTo spuBoundsTo)
     *  1).@RequestBody将这个对象转为json
     *  2).找到gulimall-coupon服务，给/coupon/spubounds/save发送请求。将上一步转的json放在请求体的位置，发送请求
     *  3).对方服务接收到请求，请求体里json数据。
     *      (@RequestBody SpuBoundsEntity spuBounds)将请求体的json转为SpuBoundsEntity。
     * 总结：只要json数据模型是兼容的，双方服务无需使用同一个to。
     * @param spuBoundsTo
     * @return
     */
    @PostMapping("/coupon/spubounds/save")
    R saveSpuBounds(@RequestBody SpuBoundsTo spuBoundsTo);

    @PostMapping("/coupon/skufullreduction/saveinfo")
    R saveSkuReduction(SkuReductionTo skuReductionTo);
}
