package com.vow.gulimall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.vow.common.exception.NoStockException;
import com.vow.common.to.mq.OrderTo;
import com.vow.common.utils.R;
import com.vow.common.vo.MemberResponseVo;
import com.vow.gulimall.order.constant.OrderConstant;
import com.vow.gulimall.order.dao.OrderItemDao;
import com.vow.gulimall.order.entity.OrderItemEntity;
import com.vow.gulimall.order.entity.PaymentInfoEntity;
import com.vow.gulimall.order.enume.OrderStatusEnum;
import com.vow.gulimall.order.feign.CartFeignService;
import com.vow.gulimall.order.feign.MemberFeignService;
import com.vow.gulimall.order.feign.ProductFeignService;
import com.vow.gulimall.order.feign.WareFeignService;
import com.vow.gulimall.order.interceptor.LoginUserInterceptor;
import com.vow.gulimall.order.service.OrderItemService;
import com.vow.gulimall.order.service.PaymentInfoService;
import com.vow.gulimall.order.to.OrderCreateTo;
import com.vow.gulimall.order.vo.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vow.common.utils.PageUtils;
import com.vow.common.utils.Query;

import com.vow.gulimall.order.dao.OrderDao;
import com.vow.gulimall.order.entity.OrderEntity;
import com.vow.gulimall.order.service.OrderService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    private ThreadLocal<OrderSubmitVo> orderSubmitVoThreadLocal = new ThreadLocal<>();

    @Autowired
    OrderItemService orderItemService;

    @Autowired
    MemberFeignService memberFeignService;

    @Autowired
    CartFeignService cartFeignService;

    @Autowired
    WareFeignService wareFeignService;

    @Autowired
    ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    PaymentInfoService paymentInfoService;

    @Autowired
    RabbitTemplate rabbitTemplate;

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

        // 5、防重令牌
        String token = UUID.randomUUID().toString().replace("-", "");

        stringRedisTemplate.opsForValue().set(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberResponseVo.getId(), token, 30, TimeUnit.MINUTES);
        orderConfirmVo.setOrderToken(token);

        CompletableFuture.allOf(getAddressFeture, getCartItemsFeture).get();

        return orderConfirmVo;
    }

    // 本地事务，在分布式系统下，只能控制自己的回滚，控制不了其他服务的回滚
    // 分布式事务：最大原因，网络问题+分布式机器。
    //@GlobalTransactional
    @Transactional
    @Override
    public SubmitOrderResponseVo submitOrder(OrderSubmitVo orderSubmitVo) {
        MemberResponseVo memberResponseVo = LoginUserInterceptor.loginUser.get();
        SubmitOrderResponseVo submitOrderResponseVo = new SubmitOrderResponseVo();
        orderSubmitVoThreadLocal.set(orderSubmitVo);
        submitOrderResponseVo.setCode(0);

        // 1、验证令牌【令牌的对比和删除必须保证原子性】
        // 返回0或1，0：令牌校验失败，1：删除失败
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        String orderToken = orderSubmitVo.getOrderToken();
        // 原子验证令牌和删除令牌
        Long result = stringRedisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class), Arrays.asList(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberResponseVo.getId()), orderToken);
        if (result == 0L) {
            // 令牌验证失败
            submitOrderResponseVo.setCode(1);
            return submitOrderResponseVo;
        } else {
            // 令牌验证成功
            // 1、下单：创建订单，验令牌，验价格，锁库存。。
            OrderCreateTo order = createOrder();
            // 2、验价
            BigDecimal payAmount = order.getOrder().getPayAmount();
            BigDecimal payPrice = orderSubmitVo.getPayPrice();
            if (Math.abs(payAmount.subtract(payPrice).doubleValue()) < 0.01) {
                // 金额对比成功
                // 3、保存订单
                saveOrder(order);
                // 4、锁定库存。只要有异常，回滚订单数据
                // 需要数据：订单号，所有订单项（skuId，skuName, num）
                WareSkuLockVo wareSkuLockVo = new WareSkuLockVo();
                wareSkuLockVo.setOrderSn(order.getOrder().getOrderSn());
                List<OrderItemVo> orderItemVos = order.getOrderItems().stream().map(orderItemEntity -> {
                    OrderItemVo orderItemVo = new OrderItemVo();
                    orderItemVo.setSkuId(orderItemEntity.getSkuId());
                    orderItemVo.setCount(orderItemEntity.getSkuQuantity());
                    orderItemVo.setTitle(orderItemEntity.getSkuName());
                    return orderItemVo;
                }).collect(Collectors.toList());
                wareSkuLockVo.setLocks(orderItemVos);
                // TODO 远程锁库存
                // 为了保证高并发，库存服务自己回滚，可以发消息给库存服务
                // 库存服务本身也可以使用自动解锁模式，使用消息队列w
                R r = wareFeignService.orderLockStock(wareSkuLockVo);
                if (r.getCode() == 0) {
                    // 锁定成功
                    submitOrderResponseVo.setOrder(order.getOrder());

                    // TODO 模拟调用优惠券服务异常
                    // int i = 10 / 0;

                    // TODO 订单创建成功，发送消息给MQ
                    rabbitTemplate.convertAndSend("order-event-exchange", "order.create.order", order.getOrder());
                    return submitOrderResponseVo;
                } else {
                    // 锁定失败
                    String msg = (String) r.get("msg");
                    throw new NoStockException(msg);
                    // submitOrderResponseVo.setCode(3);
                    // return submitOrderResponseVo;
                }
            } else {
                submitOrderResponseVo.setCode(2);
                return submitOrderResponseVo;
            }

        }


        /*String redisToken = stringRedisTemplate.opsForValue().get(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberResponseVo.getId());
        if (orderToken != null && orderToken.equals(redisToken)) {
            // 验证通过
            stringRedisTemplate.delete(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberResponseVo.getId());
        } else {
            // 不通过
        }*/
    }

    @Override
    public OrderEntity getOrderByOrderSn(String orderSn) {
        OrderEntity orderEntity = this.getOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderSn));
        return orderEntity;
    }

    @Override
    public void closeOrder(OrderEntity orderEntity) {
        // 查询这个订单的最新状态
        OrderEntity order = this.getById(orderEntity.getId());
        // 关单
        if (order.getStatus() == OrderStatusEnum.CREATE_NEW.getCode()) {
            OrderEntity updateOrder = new OrderEntity();
            updateOrder.setId(orderEntity.getId());
            updateOrder.setStatus(OrderStatusEnum.CANCLED.getCode());
            this.updateById(updateOrder);
            OrderTo orderTo = new OrderTo();
            BeanUtils.copyProperties(order, orderTo);
            // 主动发送订单取消信息给库存的MQ，要求解锁库存
            try {
                // TODO 保证消息一定会发送出去，每一个消息都可以做好日志记录（给数据库保存每一个消息的详细信息）。
                // TODO 定期扫描数据库将失败的消息重新发送
                rabbitTemplate.convertAndSend("order-event-exchange", "order.release.other.#", orderTo);
            } catch (Exception e) {
                // TODO 将没发送成功的消息进行重试发送
            }
        }
    }

    @Override
    public PayVo getOrderPay(String orderSn) {

        PayVo payVo = new PayVo();
        OrderEntity order = this.getOrderByOrderSn(orderSn);

        BigDecimal amount = order.getTotalAmount().setScale(2, BigDecimal.ROUND_UP);
        payVo.setTotal_amount(amount.toString());
        payVo.setOut_trade_no(order.getOrderSn());

        List<OrderItemEntity> order_sn = orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", orderSn));
        payVo.setSubject(order_sn.get(0).getSkuName());
        payVo.setBody(order_sn.get(0).getSkuAttrsVals());

        return payVo;
    }

    @Override
    public PageUtils queryPageWithItem(Map<String, Object> params) {
        MemberResponseVo memberResponseVo = LoginUserInterceptor.loginUser.get();
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>().eq("member_id", memberResponseVo.getId()).orderByDesc("id")
        );

        List<OrderEntity> collect = page.getRecords().stream().map(order -> {
            List<OrderItemEntity> itemEntities = orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", order.getOrderSn()));
            order.setItems(itemEntities);
            return order;
        }).collect(Collectors.toList());

        page.setRecords(collect);

        return new PageUtils(page);
    }

    /**
     * 处理支付宝的处理结果
     * @param vo
     * @return
     */
    @Override
    public String handlePayesult(PayAsyncVo vo) {
        // 1、保存交易流水
        PaymentInfoEntity paymentInfoEntity = new PaymentInfoEntity();
        paymentInfoEntity.setAlipayTradeNo(vo.getTrade_no());
        paymentInfoEntity.setOrderSn(vo.getOut_trade_no());
        paymentInfoEntity.setPaymentStatus(vo.getTrade_status());
        paymentInfoEntity.setCallbackTime(vo.getNotify_time());
        paymentInfoEntity.setTotalAmount(new BigDecimal(vo.getTotal_amount()));
        paymentInfoService.save(paymentInfoEntity);

        // 2、修改订单的状态信息
        /**
         * WAIT_BUYER_PAY   交易创建，等待买家付款
         * TRADE_CLOSED     未付款交易超时关闭，或支付完成后全额退款
         * TRADE_SUCCESS    交易成功支付
         * TRADE_FINISHED   交易结束不可退款
         */
        if (vo.getTrade_status().equals("TRADE_SUCCESS") || vo.getTrade_status().equals("TRADE_FINISHED")) {
            // 订单支付成功
            this.baseMapper.updateOrderStatus(vo.getOut_trade_no(), OrderStatusEnum.PAYED.getCode());
        }
        return "success";
    }

    /**
     * 保存订单数据
     * @param order
     */
    private void saveOrder(OrderCreateTo order) {
        OrderEntity orderEntity = order.getOrder();
        orderEntity.setModifyTime(new Date());
        this.save(orderEntity);

        List<OrderItemEntity> orderItems = order.getOrderItems();
        orderItemService.saveBatch(orderItems);
    }

    private OrderCreateTo createOrder() {
        OrderCreateTo orderCreateTo = new OrderCreateTo();
        // 1、生成一个订单号
        String orderSN = IdWorker.getTimeId();
        OrderEntity orderEntity = buildOrder(orderSN);

        // 2、获取所有订单项
        List<OrderItemEntity> orderItemEntities = buildOrderItems(orderSN);

        // 3、计算价格积分相关
        computePrice(orderEntity, orderItemEntities);

        orderCreateTo.setOrder(orderEntity);
        orderCreateTo.setOrderItems(orderItemEntities);

        return orderCreateTo;
    }

    private void computePrice(OrderEntity orderEntity, List<OrderItemEntity> orderItemEntities) {
        BigDecimal total = new BigDecimal("0.0");   // 总额
        BigDecimal promotion = new BigDecimal("0.0");   // 促销金额
        BigDecimal coupon = new BigDecimal("0.0");  // 优惠券
        BigDecimal integration = new BigDecimal("0.0"); // 积分兑换金额
        Integer giftIntegration = new Integer(0);  // 积分
        Integer giftGrowth = new Integer(0);; // 成长值
        // 订单总额
        for (OrderItemEntity orderItemEntity : orderItemEntities) {
            total = total.add(orderItemEntity.getRealAmount());
            promotion = promotion.add(orderItemEntity.getPromotionAmount());
            coupon = coupon.add(orderItemEntity.getCouponAmount());
            integration = integration.add(orderItemEntity.getIntegrationAmount());
            giftIntegration += orderItemEntity.getGiftIntegration();
            giftGrowth += orderItemEntity.getGiftGrowth();
        }
        // 1、订单价格相关
        orderEntity.setTotalAmount(total);
        // 设置应付总额
        orderEntity.setPayAmount(total.add(orderEntity.getFreightAmount()));
        orderEntity.setPromotionAmount(promotion);
        orderEntity.setCouponAmount(coupon);
        orderEntity.setIntegrationAmount(integration);
        // 设置积分和成长值
        orderEntity.setIntegration(giftIntegration);
        orderEntity.setGrowth(giftGrowth);
        orderEntity.setDeleteStatus(0); // 0：未删除
    }

    private OrderEntity buildOrder(String orderSN) {
        MemberResponseVo memberResponseVo = LoginUserInterceptor.loginUser.get();
        OrderEntity orderEntity = new OrderEntity();
        // 设置订单号
        orderEntity.setOrderSn(orderSN);
        // 设置会员id
        orderEntity.setMemberId(memberResponseVo.getId());
        orderEntity.setMemberUsername(memberResponseVo.getUsername());
        // 获取收获地址信息
        OrderSubmitVo orderSubmitVo = orderSubmitVoThreadLocal.get();
        R r = wareFeignService.getFare(orderSubmitVo.getAddrId());
        FareVo fareResponse = r.getData(new TypeReference<FareVo>() {
        });
        // 设置运费
        orderEntity.setFreightAmount(fareResponse.getFare());
        // 设置收货人信息
        orderEntity.setReceiverProvince(fareResponse.getAddress().getProvince());
        orderEntity.setReceiverCity(fareResponse.getAddress().getCity());
        orderEntity.setReceiverRegion(fareResponse.getAddress().getRegion());
        orderEntity.setReceiverDetailAddress(fareResponse.getAddress().getDetailAddress());
        orderEntity.setReceiverName(fareResponse.getAddress().getName());
        orderEntity.setReceiverPhone(fareResponse.getAddress().getPhone());
        orderEntity.setReceiverPostCode(fareResponse.getAddress().getPostCode());
        //设置订单相关状态信息
        orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        orderEntity.setAutoConfirmDay(7);

        return orderEntity;
    }

    /**
     * 构建所有订单数据
     * @param
     * @return
     */
    private List<OrderItemEntity> buildOrderItems(String orderSN) {
        // 最后确定每个购物项的价格
        List<OrderItemVo> currentUserCartItems = cartFeignService.getCurrentUserCartItems();
        if (currentUserCartItems != null && currentUserCartItems.size() > 0) {
            List<OrderItemEntity> orderItemEntities = currentUserCartItems.stream().map(cartItem -> {
                OrderItemEntity orderItemEntity = buildOrderItem(cartItem);
                orderItemEntity.setOrderSn(orderSN);
                return orderItemEntity;
            }).collect(Collectors.toList());
            return orderItemEntities;
        }
        return null;
    }

    /**
     * 构建订单项
     * @param cartItem
     * @return
     */
    private OrderItemEntity buildOrderItem(OrderItemVo cartItem) {
        OrderItemEntity orderItemEntity = new OrderItemEntity();
        // 1、订单信息：订单号
        // 2、商品的spu信息
        Long skuId = cartItem.getSkuId();
        R r = productFeignService.getSpuInfoBySkuId(skuId);
        SpuInfoVo spuInfoVo = r.getData(new TypeReference<SpuInfoVo>() {
        });
        orderItemEntity.setSpuId(spuInfoVo.getId());
        orderItemEntity.setSpuBrand(spuInfoVo.getBrandId().toString());
        orderItemEntity.setSpuName(spuInfoVo.getSpuName());
        orderItemEntity.setCategoryId(spuInfoVo.getCatalogId());
        // 3、商品的sku信息
        orderItemEntity.setSkuId(cartItem.getSkuId());
        orderItemEntity.setSkuName(cartItem.getTitle());
        orderItemEntity.setSkuPic(cartItem.getImage());
        orderItemEntity.setSkuPrice(cartItem.getPrice());
        String skuAttr = StringUtils.collectionToDelimitedString(cartItem.getSkuAttr(), ";");
        orderItemEntity.setSkuAttrsVals(skuAttr);
        orderItemEntity.setSkuQuantity(cartItem.getCount());
        // 4、商品的优惠信息
        orderItemEntity.setPromotionAmount(new BigDecimal("0"));
        orderItemEntity.setCouponAmount(new BigDecimal("0"));
        orderItemEntity.setIntegrationAmount(new BigDecimal("0"));
        // 5、积分信息
        orderItemEntity.setGiftGrowth(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString())).intValue());
        orderItemEntity.setGiftIntegration(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString())).intValue());
        // 6、订单项的价格信息
        BigDecimal originAmount = orderItemEntity.getSkuPrice().multiply(new BigDecimal(orderItemEntity.getSkuQuantity().toString()));
        BigDecimal realAmount = originAmount.subtract(orderItemEntity.getPromotionAmount()).subtract(orderItemEntity.getCouponAmount()).subtract(orderItemEntity.getIntegrationAmount());
        orderItemEntity.setRealAmount(realAmount);
        return orderItemEntity;
    }

}