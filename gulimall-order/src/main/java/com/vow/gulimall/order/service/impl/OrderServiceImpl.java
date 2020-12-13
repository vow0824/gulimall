package com.vow.gulimall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.vow.common.utils.R;
import com.vow.common.vo.MemberResponseVo;
import com.vow.gulimall.order.feign.CartFeignService;
import com.vow.gulimall.order.feign.MemberFeignService;
import com.vow.gulimall.order.feign.WareFeignService;
import com.vow.gulimall.order.interceptor.LoginUserInterceptor;
import com.vow.gulimall.order.vo.MemberAddressVo;
import com.vow.gulimall.order.vo.OrderConfirmVo;
import com.vow.gulimall.order.vo.OrderItemVo;
import com.vow.gulimall.order.vo.SkuStockVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vow.common.utils.PageUtils;
import com.vow.common.utils.Query;

import com.vow.gulimall.order.dao.OrderDao;
import com.vow.gulimall.order.entity.OrderEntity;
import com.vow.gulimall.order.service.OrderService;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    @Autowired
    MemberFeignService memberFeignService;

    @Autowired
    CartFeignService cartFeignService;

    @Autowired
    WareFeignService wareFeignService;

    @Autowired
    ThreadPoolExecutor threadPoolExecutor;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException {
        MemberResponseVo memberResponseVo = LoginUserInterceptor.loginUser.get();
        OrderConfirmVo orderConfirmVo = new OrderConfirmVo();

        // 获取之前的请求
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        CompletableFuture<Void> getAddressFeture = CompletableFuture.runAsync(() -> {
            // 每一个线程都来共享之前请求来的的数据
            RequestContextHolder.setRequestAttributes(requestAttributes);
            // 1、远程查询所有的收获地址列表
            List<MemberAddressVo> address = memberFeignService.getAddress(memberResponseVo.getId());
            orderConfirmVo.setAddress(address);
        }, threadPoolExecutor);

        CompletableFuture<Void> getCartItemsFeture = CompletableFuture.runAsync(() -> {
            // 每一个线程都来共享之前请求来的的数据
            RequestContextHolder.setRequestAttributes(requestAttributes);
            // 2、远程查询购物车所有选中的购物项
            List<OrderItemVo> currentUserCartItems = cartFeignService.getCurrentUserCartItems();
            orderConfirmVo.setItems(currentUserCartItems);
            // feign在远程调用之前要构造请求，会调用很多拦截器（RequestInterceptor）
        }, threadPoolExecutor).thenRunAsync(() -> {
            List<OrderItemVo> items = orderConfirmVo.getItems();
            List<Long> collect = items.stream().map(item -> item.getSkuId()).collect(Collectors.toList());
            R r = wareFeignService.getSkuHasStock(collect);
            List<SkuStockVo> data = r.getData(new TypeReference<List<SkuStockVo>>() {
            });
            if (data != null && data.size() > 0) {
                Map<Long, Boolean> map = data.stream().collect(Collectors.toMap(SkuStockVo::getSkuId, SkuStockVo::getHasStock));
                orderConfirmVo.setStocks(map);
            }
        }, threadPoolExecutor);


        // 3、查询用户积分
        Integer integration = memberResponseVo.getIntegration();
        orderConfirmVo.setIntegration(integration);

        // 4、其他数据自动计算

        // TODO 5、放重令牌

        CompletableFuture.allOf(getAddressFeture, getCartItemsFeture).get();

        return orderConfirmVo;
    }

}