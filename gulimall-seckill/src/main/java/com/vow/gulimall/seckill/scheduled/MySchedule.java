package com.vow.gulimall.seckill.scheduled;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时任务
 *  1、@EnableScheduling 开启定时任务
 *  2、@Component 加入容器中
 *  3、@Scheduled 开一一个定时任务
 *  4、TaskSchedulingAutoConfiguration 自动配置类
 *
 * 异步任务
 *  1、@EnableAsync 开启异步任务
 *  2、@Async 异步执行方法
 *  3、TaskExecutionAutoConfiguration 自动配置类
 */
@Slf4j
//@Component
//@EnableAsync
//@EnableScheduling
public class MySchedule {

    /**
     * 1、spring中不支持年份
     * 2、spring中，周的位置 1-7 分别代表周一到周日 MON-SUN
     * 3、定时任务不应该阻塞
     *  1）、可以让业务以异步的方式运行，自己提交到线程池
     *  2）、支持定时任务线程池：设置TaskSchedulingProperties
     *  3）、让定时任务异步执行
     * 使用异步+定时任务来完成定时任务不阻塞的功能
     */
    /*@Async
    @Scheduled(cron = "* * * * * ?")
    public void hello() {
        log.info("hello....");
    }*/
}
