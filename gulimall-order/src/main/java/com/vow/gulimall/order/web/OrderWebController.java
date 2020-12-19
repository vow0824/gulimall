package com.vow.gulimall.order.web;

import com.vow.common.exception.NoStockException;
import com.vow.gulimall.order.service.OrderService;
import com.vow.gulimall.order.vo.OrderConfirmVo;
import com.vow.gulimall.order.vo.OrderSubmitVo;
import com.vow.gulimall.order.vo.SubmitOrderResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.concurrent.ExecutionException;

@Controller
public class OrderWebController {

    @Autowired
    OrderService orderService;

    /*@GetMapping("/{page}.html")
    public String hello(@PathVariable("page") String page) {
        return page;
    }*/

    @GetMapping("/toTrade")
    public String toTrade(Model model) throws ExecutionException, InterruptedException {
        OrderConfirmVo orderConfirmVo = orderService.confirmOrder();
        model.addAttribute("orderConfirmData", orderConfirmVo);
        return "confirm";
    }

    /**
     * 下单功能
     * @param orderSubmitVo
     * @return
     */
    @PostMapping("/submitOrder")
    public String submitOrder(OrderSubmitVo orderSubmitVo, Model model, RedirectAttributes redirectAttributes) {
        // 下单成功来到支付页
        // 下单失败回到订单确认页重新确认订单信息
        try {
            SubmitOrderResponseVo submitOrderResponseVo = orderService.submitOrder(orderSubmitVo);
            if (submitOrderResponseVo.getCode() == 0) {
                // 下单支付页
                model.addAttribute("submitOrderResponse", submitOrderResponseVo);
                return "pay";
            } else {
                String msg = "下单失败：";
                switch (submitOrderResponseVo.getCode()) {
                    case 1 : msg += "订单信息过期，请刷新再次提交"; break;
                    case 2 : msg += "订单商品价格发生变化，请确认后再次提交"; break;
                    case 3 : msg += "商品库存不足"; break;
                }
                redirectAttributes.addFlashAttribute("msg", msg);
                return "redirect:http://order.gulimall.com/toTrade";
            }
        } catch (NoStockException e) {
            redirectAttributes.addFlashAttribute("msg", "商品库存不足");
            return "redirect:http://order.gulimall.com/toTrade";
        }

    }
}
