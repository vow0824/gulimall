package com.vow.gulimall.cart.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.vow.common.utils.R;
import com.vow.gulimall.cart.feign.ProductFeignService;
import com.vow.gulimall.cart.interceptor.CartInterceptor;
import com.vow.gulimall.cart.service.CartService;
import com.vow.gulimall.cart.vo.Cart;
import com.vow.gulimall.cart.vo.CartItem;
import com.vow.gulimall.cart.vo.SkuInfoVo;
import com.vow.gulimall.cart.vo.UserInfoTo;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.C;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CartServiceImpl implements CartService {

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    ThreadPoolExecutor threadPoolExecutor;

    private final String CART_PREFIX = "gulimall:cart:";

    @Override
    public CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();

        String sku = (String) cartOps.get(skuId.toString());
        if (StringUtils.isEmpty(sku)) {
            // 购物车无此商品
            CartItem cartItem = new CartItem();
            // 添加新商品到购物车
            CompletableFuture<Void> getSkuInfoTask = CompletableFuture.runAsync(() -> {
                // 1、远程查询当前要添加的商品的信息
                R r = productFeignService.getSkuInfo(skuId);
                SkuInfoVo skuInfo = r.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                });
                // 2、商品添加到购物车

                cartItem.setCheck(true);
                cartItem.setCount(num);
                cartItem.setImage(skuInfo.getSkuDefaultImg());
                cartItem.setTitle(skuInfo.getSkuTitle());
                cartItem.setSkuId(skuId);
                cartItem.setPrice(skuInfo.getPrice());
            }, threadPoolExecutor);

            CompletableFuture<Void> getSkuSaleAttrValuesTask = CompletableFuture.runAsync(() -> {
                // 3、远程查询sku的组合信息
                R r = productFeignService.getSkuSaleAttrValues(skuId);
                List<String> SkuSaleAttrValues = r.getData("data", new TypeReference<List<String>>() {
                });
                cartItem.setSkuAttr(SkuSaleAttrValues);
            }, threadPoolExecutor);

            CompletableFuture.allOf(getSkuInfoTask, getSkuSaleAttrValuesTask).get();
            cartOps.put(skuId.toString(), JSON.toJSONString(cartItem));
            return cartItem;
        } else {
            // 购物车有此商品，修改数量即可
            CartItem cartItem = JSON.parseObject(sku, CartItem.class);
            cartItem.setCount(cartItem.getCount() + num);
            cartOps.put(skuId.toString(), JSON.toJSONString(cartItem));

            return cartItem;
        }

    }

    @Override
    public CartItem getCartItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        String str = (String) cartOps.get(skuId.toString());
        CartItem cartItem = JSON.parseObject(str, CartItem.class);
        return cartItem;
    }

    @Override
    public Cart getCart() throws ExecutionException, InterruptedException {
        Cart cart = new Cart();
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        if (!StringUtils.isEmpty(userInfoTo.getUserId())) {
            // 已登录
            String cartKey = CART_PREFIX + userInfoTo.getUserId();
            String tempCarttKey = CART_PREFIX + userInfoTo.getUserKey();
            // 需要判断临时购物车中是否有数据
            List<CartItem> tempCartItems = getCartItems(tempCarttKey);
            if (tempCartItems != null && tempCartItems.size() > 0) {
                // 临时购物车有数据，需要合并
                for (CartItem tempCartItem : tempCartItems) {
                    addToCart(tempCartItem.getSkuId(), tempCartItem.getCount());
                }
                // 清除临时购物车的数据
                clearCart(tempCarttKey);
            }
            // 再来获取登录后的购物车数据， 包含合并过来的临时购物车的数据
            List<CartItem> cartItems = getCartItems(cartKey);
            cart.setItems(cartItems);
        } else {
            // 未登录，获取临时购物车的所有购物项
            String cartKey = CART_PREFIX + userInfoTo.getUserKey();
            List<CartItem> cartItems = getCartItems(cartKey);
            cart.setItems(cartItems);
        }
        return cart;
    }

    @Override
    public void clearCart(String cartKey) {
        stringRedisTemplate.delete(cartKey);
    }

    @Override
    public void checkItemm(Long skuId, Integer check) {
        CartItem cartItem = getCartItem(skuId);
        cartItem.setCheck(check == 1 ? true : false);

        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.put(skuId.toString(), JSON.toJSONString(cartItem));
    }

    @Override
    public void changeItemCount(Long skuId, Integer num) {
        CartItem cartItem = getCartItem(skuId);
        cartItem.setCount(num);

        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.put(skuId.toString(), JSON.toJSONString(cartItem));
    }

    @Override
    public void deleteItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.delete(skuId.toString());
    }

    @Override
    public List<CartItem> getUserCartItems() {
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        if (userInfoTo == null) {
            return null;
        }

        String cartKey = CART_PREFIX + userInfoTo.getUserId();
        // 获取所有选中的购物项
        List<CartItem> cartItems = getCartItems(cartKey).stream().filter(cartItem -> cartItem.getCheck()).map(item -> {
            // 更新为最新价格
            R price = productFeignService.getPrice(item.getSkuId());
            String data = (String) price.get("data");
            item.setPrice(new BigDecimal(data));
            return item;
        }).collect(Collectors.toList());
        return cartItems;
    }

    /**
     * 获取到我们要操作的购物车
     * @return
     */
    private BoundHashOperations<String, Object, Object> getCartOps() {
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        String cartKey = "";
        if (userInfoTo.getUserId() != null) {
            // gulimall:cart:1
            cartKey = CART_PREFIX + userInfoTo.getUserId();
        } else{
            cartKey = CART_PREFIX + userInfoTo.getUserKey();
        }
        BoundHashOperations<String, Object, Object> ops = stringRedisTemplate.boundHashOps(cartKey);
        return ops;
    }

    private List<CartItem> getCartItems(String cartKey) {
        BoundHashOperations<String, Object, Object> ops = stringRedisTemplate.boundHashOps(cartKey);
        List<Object> values = ops.values();
        if (values != null && values.size() > 0) {
            List<CartItem> collect = values.stream().map(obj -> {
                String str = (String) obj;
                CartItem cartItem = JSON.parseObject(str, CartItem.class);
                return cartItem;
            }).collect(Collectors.toList());
            return collect;
        }
        return null;
    }



}
