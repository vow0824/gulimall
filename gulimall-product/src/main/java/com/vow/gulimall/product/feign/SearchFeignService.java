package com.vow.gulimall.product.feign;

import com.vow.common.to.es.SkuEsModel;
import com.vow.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient("gulimall-search")
public interface SearchFeignService {

    @PostMapping("/search/save/product")
    public R productStatusUp(@RequestBody List<SkuEsModel> skuEsModels);
}
