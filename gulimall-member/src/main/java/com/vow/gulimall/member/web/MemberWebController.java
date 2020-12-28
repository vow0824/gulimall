package com.vow.gulimall.member.web;

import com.vow.common.utils.R;
import com.vow.gulimall.member.feign.OrderFeignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.Map;

@Controller
public class MemberWebController {

    @Autowired
    OrderFeignService orderFeignService;

    @GetMapping("/memberOrder.html")
    public String memberOrderPage(@RequestParam(value = "page", defaultValue = "1") String pageNum, Model model) {
        // 获取到支付宝给我们传来的所有请求数据，验证签名，如果正确，修改订单状态
        Map<String, Object> params = new HashMap<>();
        params.put("page", pageNum);
        R r = orderFeignService.listWithItem(params);
        model.addAttribute("orders", r);
        // 查出当前登录用户的所有订单数据列表
        return "orderList";
    }
}
