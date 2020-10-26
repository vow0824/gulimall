package com.vow.gulimall.product;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 1、整合redis
 *  1）、引入data-redis-starter
 *  2）、简单配置redis的host信息
 *  3）、使用springboot配置好的StringRedisTemplate来操作redis
 *
 * 2、整合redission作为分布式锁等功能框架
 *  1）、引入依赖redisson
 *  2）、配置redission
 *
 * 3、整合spring-cache
 *  1）、spring-boot-starter-cache
 *  2）、写配置
 *      （1）、自动配置了哪些
 *          CacheAutoConfiguration会导入RedisCacheConfiguration
 *          RedisCacheConfiguration自动配好了缓存管理器
 *      （2）、配置使用redis作为缓存
 *      （3）、测试使用缓存
 *          @Cacheable 触发将数据保存到缓存的操作
 *          @CacheEvict 触发将数据从缓存删除的操作
 *          @CachePut 不影响方法执行更新缓存
 *          @Caching 组合以上多个操作
 *          @CacheConfig 在类级别共享缓存的相同配置
 *          1）、开启缓存功能@EnableCaching
 *          2）、只需要注解就能完成缓存操作
 *      （4）、原理
 *          CacheAutoConfiguration -> RedisCacheConfiguration -> 自动配置了RedisCacheManager -> 初始化所有的缓存 ->
 *          每个缓存决定使用什么配置 -> 如果 redisCacheConfiguration有就用自己的，没有就用默认配置 ->
 *          想要改缓存的配置，只需要给容器中放一个RedisCacheConfiguration即可
 *          -> 就会应用到RedisCacheManager管理的所有缓存分区中
 */

@EnableFeignClients(basePackages = "com.vow.gulimall.product.feign")
@MapperScan("com.vow.gulimall.product.dao")
@SpringBootApplication
@EnableDiscoveryClient
public class GulimallProductApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallProductApplication.class, args);
    }

}
