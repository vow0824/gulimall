package com.vow.gulimall.seckill.scheduled;

import com.vow.gulimall.seckill.service.SeckillService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 秒杀商品的定时上架：
 *  每天晚上3点上架最近三天参与秒杀的商品
 *  当天00:00:00 - 23:59:59
 *  明天00:00:00 - 23:59:59
 *  后天00:00:00 - 23:59:59
 */
@Slf4j
@Service
public class SeckillSkuScheduled {

    @Autowired
    SeckillService seckillService;

    @Scheduled(cron = "0 * * * * ?")
    public void uploadSeckillSkuLatestThreeDays() {
        // 1、重复上架无需处理
        seckillService.uploadSeckillSkuLatestThreeDays();
    }
}
