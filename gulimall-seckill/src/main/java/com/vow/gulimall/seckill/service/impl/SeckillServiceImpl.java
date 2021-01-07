package com.vow.gulimall.seckill.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.vow.common.utils.R;
import com.vow.common.vo.MemberResponseVo;
import com.vow.gulimall.seckill.feign.CouponFeignService;
import com.vow.gulimall.seckill.feign.ProductFeignService;
import com.vow.gulimall.seckill.interceptor.LoginUserInterceptor;
import com.vow.gulimall.seckill.service.SeckillService;
import com.vow.gulimall.seckill.to.SeckillSkuRedisTo;
import com.vow.gulimall.seckill.vo.SeckillSessionsWithSkusVo;
import com.vow.gulimall.seckill.vo.SeckillSkuVo;
import com.vow.gulimall.seckill.vo.SkuInfoVo;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class SeckillServiceImpl implements SeckillService {

    @Autowired
    CouponFeignService couponFeignService;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    RedissonClient redissonClient;

    private final String SESSION_PREFIX = "seckill:sessions:";

    private final String SKUKILL_CACHE_PREFIX = "seckill:skus";

    private final String SKU_STOCK_SEMAPHORE = "seckill:stock:";

    @Override
    public void uploadSeckillSkuLatestThreeDays() {
        // 1、扫描需要参与秒杀的活动
        R r = couponFeignService.getLatestThreeDaysSession();
        if (r.getCode() == 0) {
            // 获取上架商品
            List<SeckillSessionsWithSkusVo> sessions = r.getData(new TypeReference<List<SeckillSessionsWithSkusVo>>() {
            });
            // 缓存到redis
            // 1、缓存活动信息
            saveSessionInfos(sessions);
            // 2、缓存活动关联的商品信息
            saveSessionSkuInfos(sessions);
        }
    }

    /**
     * f返回当前可参与秒杀的商品
     * @return
     */
    @Override
    public List<SeckillSkuRedisTo> getCurrentSeckillSkus() {
        // 1、确定当前时间属于那个场次
        long time = new Date().getTime();
        System.out.println(time);

        Set<String> keys = stringRedisTemplate.keys(SESSION_PREFIX + "*");
        for (String key : keys) {
            // seckill:sessions:1610010000000-1610017200000
            String replace = key.replace(SESSION_PREFIX, "");
            String[] split = replace.split("-");
            Long start = Long.parseLong(split[0]);
            Long end = Long.parseLong(split[1]);
            System.out.println(time + "---" + start + "---" + end);

            if (time >= start && time <= end) {
                // 2、获取这个秒杀场次需要的所有商品信息a
                List<String> range = stringRedisTemplate.opsForList().range(key, -100, 100);
                BoundHashOperations<String, String, String> hashOps =stringRedisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
                List<String> list = hashOps.multiGet(range);
                if (list != null && list.size() > 0) {
                    List<SeckillSkuRedisTo> collect = list.stream().map(item -> {
                        SeckillSkuRedisTo seckillSkuRedisTo = JSON.parseObject(item, SeckillSkuRedisTo.class);
                        // seckillSkuRedisTo.setRandomCode(null);   // 当前秒杀开始了，需要随机码
                        return seckillSkuRedisTo;
                    }).collect(Collectors.toList());
                    return collect;
                }
                break;
            }
        }


        return null;
    }

    @Override
    public SeckillSkuRedisTo getSkuSeckillInfo(Long skuId) {
        // 1、找到所有参与需要参与秒杀是商品key
        BoundHashOperations<String, String, String> hashOps = stringRedisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
        Set<String> keys = hashOps.keys();
        if (keys != null && keys.size() > 0) {
            String regx= "\\d-" + skuId;
            for (String key : keys) {
                if (Pattern.matches(regx, key)) {
                    String json = hashOps.get(key);
                    SeckillSkuRedisTo skuRedisTo = JSON.parseObject(json, SeckillSkuRedisTo.class);
                    long now = new Date().getTime();
                    if (now < skuRedisTo.getStartTime() || now > skuRedisTo.getEndTime()) {
                        skuRedisTo.setRandomCode("");
                    }
                    return skuRedisTo;
                }
            }
        }
        return null;
    }

    @Override
    public String kill(String killId, String key, Integer num) {
        MemberResponseVo memberResponseVo = LoginUserInterceptor.loginUser.get();

        // 1、获取当前秒杀商品的详细信息
        BoundHashOperations<String, String, String> hashOps = stringRedisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
        String s = hashOps.get(key);
        if (!StringUtils.isEmpty(s)) {
            SeckillSkuRedisTo skuRedisTo = JSON.parseObject(s, SeckillSkuRedisTo.class);
            // 校验合法性
            long now = new Date().getTime();
            if (now >= skuRedisTo.getStartTime() && now <= skuRedisTo.getEndTime()) {
                // 2、校验随机码和商品id
                String randomCode = skuRedisTo.getRandomCode();
                String seckillId = skuRedisTo.getPromotionSessionId().toString() + "-" + skuRedisTo.getSkuId().toString();
                if (seckillId.equals(killId) && randomCode.equals(key)) {
                    // 3、验证购物数量
                    if (num <= skuRedisTo.getSeckillLimit()) {
                        // 4、验证这个人是否已经购买过。幂等性处理；只要秒杀成功，就去占位。userId_sessionId_skuId
                        // SETNX
                        String redisKey = memberResponseVo.getId() + "_" + seckillId;
                        // 过期时间 = 活动结束时间 - 当前时间
                        long ttl = skuRedisTo.getEndTime() - now;
                        // 自动过期
                        Boolean aBoolean = stringRedisTemplate.opsForValue().setIfAbsent(redisKey, num.toString(), ttl, TimeUnit.MILLISECONDS);
                        if (aBoolean) {
                            // 占位成功说明没有购买过
                            RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + randomCode);
                            try {
                                boolean b = semaphore.tryAcquire(num, 100, TimeUnit.MILLISECONDS);
                                // 秒杀成功
                                // 快速下单。发送MQ消息
                                return IdWorker.getTimeId();
                            } catch (InterruptedException e) {
                                return null;
                            }
                        }
                        // 占位失败，说明购买过
                        return null;
                    }
                    return null;
                }
                return null;
            }
            return null;
        }
        return null;
    }

    private void saveSessionInfos(List<SeckillSessionsWithSkusVo> sessions) {
        sessions.stream().forEach(session -> {
            Long startTime = session.getStartTime().getTime();
            Long endTime = session.getEndTime().getTime();
            String key = SESSION_PREFIX + startTime + "-" + endTime;
            // 缓存活动信息
            if (!stringRedisTemplate.hasKey(key)) {
                List<String> collect = session.getRelationSkus().stream().map(item -> item.getId() + "-" + item.getSkuId().toString()).collect(Collectors.toList());
                stringRedisTemplate.opsForList().leftPushAll(key, collect);
            }
        });
    }

    private void saveSessionSkuInfos(List<SeckillSessionsWithSkusVo> sessions) {
        sessions.stream().forEach(session -> {
            // 准备hash操作
            BoundHashOperations<String, Object, Object> hashOps = stringRedisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
            session.getRelationSkus().stream().forEach(item -> {
                if (!hashOps.hasKey(item.getPromotionSessionId() + "-" + item.getSkuId().toString())) {
                    // 缓存商品
                    SeckillSkuRedisTo seckillSkuRedisTo = new SeckillSkuRedisTo();
                    // 1、sku的基本信息
                    R r = productFeignService.getSkuInfo(item.getSkuId());
                    if (r.getCode() == 0) {
                        SkuInfoVo skuInfo = r.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                        });
                        seckillSkuRedisTo.setSkuInfo(skuInfo);
                    }
                    // 2、sku的秒杀信息
                    BeanUtils.copyProperties(item, seckillSkuRedisTo);
                    // 3、设置当前商品秒杀的时间信息
                    seckillSkuRedisTo.setStartTime(session.getStartTime().getTime());
                    seckillSkuRedisTo.setEndTime(session.getEndTime().getTime());

                    // 4、设置商品的随机码
                    String randomCode = UUID.randomUUID().toString().replace("-", "");
                    seckillSkuRedisTo.setRandomCode(randomCode);
                    hashOps.put(item.getPromotionSessionId() + "-" + item.getSkuId().toString(), JSON.toJSONString(seckillSkuRedisTo));

                    // 5、引入分布式信号量，将商品可以秒杀的件数作为信号量   限流
                    redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + randomCode).trySetPermits(item.getSeckillCount());
                }
            });
        });
    }
}
