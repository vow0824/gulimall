package com.vow.gulimall.product.fallback;

import com.vow.common.exception.BizCodeEnum;
import com.vow.common.utils.R;
import com.vow.gulimall.product.feign.SeckillFeignService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SeckillFeignServiceFallback implements SeckillFeignService {
    @Override
    public R getSkuSeckillInfo(Long skuId) {
        log.info("调用SeckillFeignService的熔断方法");
        return R.error(BizCodeEnum.TOO_MANY_REQUEST.getCode(), BizCodeEnum.TOO_MANY_REQUEST.getMsg());
    }
}
