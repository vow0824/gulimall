package com.vow.gulimall.cart.controller;

import com.vow.common.constant.AuthServerConstant;
import com.vow.gulimall.cart.interceptor.CartInterceptor;
import com.vow.gulimall.cart.service.CartService;
import com.vow.gulimall.cart.vo.Cart;
import com.vow.gulimall.cart.vo.CartItem;
import com.vow.gulimall.cart.vo.UserInfoTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.util.concurrent.ExecutionException;

@Controller
public class CartController {

    @Autowired
    CartService cartService;

    /**
     * 浏览器有一个cookie：user-key 标识用户身份有一个月的过期时间
     * 如果第一次使用jd的购物车功能，都会给用户一个临时的用户身份
     * 浏览器以后保存，每次访问都会带上这个cookie：user-key
     *
     * 登录：session
     * 没登录：cookie：user-key
     * 第一次如果没有临时用户，帮忙创建一个临时用户
     * @return
     */
    @GetMapping("/cart.html")
    public String cartListPage(HttpSession session, Model model) throws ExecutionException, InterruptedException {
        Cart cart = cartService.getCart();
        model.addAttribute("cart", cart);

        return  "cartList";
    }

    /**
     * 添加商品到购物车
     * RedirectAttributes redirectAttributes
     *  redirectAttributes.addFlashAttribute()：将数据放在session里面，可以在页面中取出，但是只能取一次
     *  redirectAttributes.addAttribute()：将数据拼接在url后面
     * @return
     */
    @GetMapping("/addToCart")
    public String addToCart(@RequestParam("skuId") Long skuId, @RequestParam("num") Integer num, RedirectAttributes redirectAttributes) throws ExecutionException, InterruptedException {
        cartService.addToCart(skuId, num);
        redirectAttributes.addAttribute("skuId", skuId);
        return "redirect:http://cart.gulimall.com/addToCartSuccess.html";
    }

    @GetMapping("/addToCartSuccess.html")
    public String addToCartSuccessPage(@RequestParam("skuId") Long skuId, Model model) {
        // 再次查询购物车即可
        CartItem cartItem = cartService.getCartItem(skuId);
        model.addAttribute("item", cartItem);
        return "success";
    }
}
