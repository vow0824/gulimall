package com.vow.gulimall.seckill.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.vow.common.utils.R;
import com.vow.gulimall.seckill.feign.CouponFeignService;
import com.vow.gulimall.seckill.feign.ProductFeignService;
import com.vow.gulimall.seckill.service.SeckillService;
import com.vow.gulimall.seckill.to.SeckillSkuRedisTo;
import com.vow.gulimall.seckill.vo.SeckillSessionsWithSkusVo;
import com.vow.gulimall.seckill.vo.SeckillSkuVo;
import com.vow.gulimall.seckill.vo.SkuInfoVo;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.UUID;
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

    private void saveSessionInfos(List<SeckillSessionsWithSkusVo> sessions) {
        sessions.stream().forEach(session -> {
            Long startTime = session.getStartTime().getTime();
            Long endTime = session.getEndTime().getTime();
            String key = SESSION_PREFIX + startTime + "-" + endTime;
            List<String> collect = session.getRelationSkus().stream().map(item -> item.getId().toString()).collect(Collectors.toList());
            // 缓存活动信息
            stringRedisTemplate.opsForList().leftPushAll(key, collect);
        });
    }

    private void saveSessionSkuInfos(List<SeckillSessionsWithSkusVo> sessions) {
        sessions.stream().forEach(session -> {
            // 准备hash操作
            BoundHashOperations<String, Object, Object> hashOps = stringRedisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
            session.getRelationSkus().stream().forEach(item -> {
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
                // 5、引入分布式信号量，将商品可以秒杀的件数作为信号量   限流
                redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + randomCode).trySetPermits(item.getSeckillCount());
                hashOps.put(item.getId(), JSON.toJSONString(seckillSkuRedisTo));
            });
        });
    }
}
