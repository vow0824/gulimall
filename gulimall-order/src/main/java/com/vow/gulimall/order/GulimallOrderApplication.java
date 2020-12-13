package com.vow.gulimall.order;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
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
 */
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
