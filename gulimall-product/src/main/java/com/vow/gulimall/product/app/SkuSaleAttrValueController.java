package com.vow.gulimall.product.app;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.vow.gulimall.product.entity.SkuSaleAttrValueEntity;
import com.vow.gulimall.product.service.SkuSaleAttrValueService;
import com.vow.common.utils.PageUtils;
import com.vow.common.utils.R;



/**
 * sku销售属性&值
 *
 * @author wushaopeng
 * @email wushaopeng@gmail.com
 * @date 2020-09-16 10:21:10
 */
@RestController
@RequestMapping("product/skusaleattrvalue")
public class SkuSaleAttrValueController {
    @Autowired
    private SkuSaleAttrValueService skuSaleAttrValueService;

    @GetMapping("/stringlist/{skuId}")
    public R getSkuSaleAttrValues(@PathVariable("skuId") Long skuId) {
        List<String> skuSaleAttrValuesList = skuSaleAttrValueService.getSkuSaleAttrValuesAsStringList(skuId);
        return R.ok().put("data", skuSaleAttrValuesList);
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    // @RequiresPermissions("product:skusaleattrvalue:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = skuSaleAttrValueService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    // @RequiresPermissions("product:skusaleattrvalue:info")
    public R info(@PathVariable("id") Long id){
		SkuSaleAttrValueEntity skuSaleAttrValue = skuSaleAttrValueService.getById(id);

        return R.ok().put("skuSaleAttrValue", skuSaleAttrValue);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    // @RequiresPermissions("product:skusaleattrvalue:save")
    public R save(@RequestBody SkuSaleAttrValueEntity skuSaleAttrValue){
		skuSaleAttrValueService.save(skuSaleAttrValue);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    // @RequiresPermissions("product:skusaleattrvalue:update")
    public R update(@RequestBody SkuSaleAttrValueEntity skuSaleAttrValue){
		skuSaleAttrValueService.updateById(skuSaleAttrValue);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    // @RequiresPermissions("product:skusaleattrvalue:delete")
    public R delete(@RequestBody Long[] ids){
		skuSaleAttrValueService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
