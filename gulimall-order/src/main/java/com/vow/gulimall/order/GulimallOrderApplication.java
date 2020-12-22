package com.vow.gulimall.order;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * 使用RabbitMQ
 * 1、引入amqp场景：RabbitAutoConfiguration就会自动生效
 * 2、给容器中自动配置了 RabbitTemplate、AmqpAdmin、CachingConnectionFactory、RabbitMessagingTemplate
 * 3、给配置文件中配置spring.rabbitmq.xxx信息
 * 4、@EnableRabbit
 * 5、监听消息：使用@RabbitListener，必须有@EnableRabbit
 *  @RabbitListener：类+方法上（监听哪些队列即可）
 *  @RabbitHandler：标在方法上（重载区分不同的消息类型）
 *
 *
 * 本地事务失效问题
 * 同一个对象内事务方法互调默认失效，原因：绕过了代理对象，事务是使用代理对象来控制的
 * 解决：使用代理对象来调用事务方法
 *  1）、引入spring-boot-starter-aop;引入了aspectj
 *  2）、@EnableAspectJAutoProxy(exposeProxy = true)开启aspectj动态代理功能，以后所有的动态代理都是由aspectj创建的（即使没有接口也可以创建动态代理）。
 *      对外暴露代理对象
 *  3）、本类互相调用
 *
 *  seata控制分布式事务
 *  1）、每一个微服务先必须创建undo_log表
 *  2）、安装事务协调器：seata server： https://github.com/seata/seata/releases
 *  3）、整合
 *      1、引入依赖：spring-cloud-starter-alibaba-seata seata-all
 *      2、启动seata-server
 *          registry.conf：注册中心配置；修改registry type=nacos
 *          file.conf：
 *      3、所有想要用到分布式事务的微服务，使用seata DataSourceProxy代理自己的数据源
 *      4、每个微服务，都必须导入 file.conf     registry.conf
 *      5、给分布式大事务的入口标注@GlobalTeansactional
 *      6、每一个远程的小事务用@Transactional
 */
@EnableAspectJAutoProxy(exposeProxy = true)
@EnableFeignClients
@EnableRedisHttpSession
@EnableDiscoveryClient
@EnableRabbit
@SpringBootApplication
public class GulimallOrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallOrderApplication.class, args);
    }

}
