package com.vow.gulimall.seckill.controller;

import com.vow.common.utils.R;
import com.vow.gulimall.seckill.service.SeckillService;
import com.vow.gulimall.seckill.to.SeckillSkuRedisTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class SeckillController {

    @Autowired
    private SeckillService seckillService;

    /**
     * 返回当前时间可以参与秒杀的商品
     * @return
     */
    @ResponseBody
    @GetMapping("/currentSeckillSkus")
    public R getCurrentSeckillSkus() {
        List<SeckillSkuRedisTo> skuRedisTos = seckillService.getCurrentSeckillSkus();
        return R.ok().setData(skuRedisTos);
    }

    @ResponseBody
    @GetMapping("/sku/seckill/{skuId}")
    public R getSkuSeckillInfo(@PathVariable("skuId") Long skuId) {
        SeckillSkuRedisTo skuRedisTo = seckillService.getSkuSeckillInfo(skuId);
        return R.ok().setData(skuRedisTo);
    }

    @GetMapping("/kill")
    public String seckill(@RequestParam("killId") String killId, @RequestParam("key") String key, @RequestParam("num") Integer num, Model model) {
        String orderSn = seckillService.kill(killId, key, num);
        System.out.println(orderSn);
        model.addAttribute("orderSn", orderSn);
        return "success";
    }
}
