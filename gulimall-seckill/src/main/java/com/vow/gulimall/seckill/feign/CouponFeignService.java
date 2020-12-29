package com.vow.gulimall.seckill.feign;

import com.vow.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient("gulimall-coupon")
public interface CouponFeignService {

    @GetMapping("/coupon/seckillsession/latestThreeDaysSession")
    R getLatestThreeDaysSession();
}
