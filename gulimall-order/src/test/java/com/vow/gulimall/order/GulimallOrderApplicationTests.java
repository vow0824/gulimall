package com.vow.gulimall.order;

import com.vow.gulimall.order.entity.OrderEntity;
import com.vow.gulimall.order.entity.OrderReturnReasonEntity;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;
import java.util.UUID;

@Slf4j
@SpringBootTest
class GulimallOrderApplicationTests {

    @Test
    void contextLoads() {
    }

    @Autowired
    AmqpAdmin amqpAdmin;

    @Autowired
    RabbitTemplate rabbitTemplate;

    /**
     * 1、如何创建Exchange(hello.java.exchange)、Queue、Binding
     *  1）、使用AmqpAdmin进行创建
     * 2、如何收发消息
     */
    @Test
    public void createExchange() {
        /**
         * String name, boolean durable, boolean autoDelete, Map<String, Object> arguments
         */
        DirectExchange directExchange = new DirectExchange("hello-java-exchange", true, false);
        amqpAdmin.declareExchange(directExchange);
        log.info("Exchange[{}]创建成功", "hello-java-exchange");
    }

    @Test
    public void createQueue() {
        /**
         * Queue(String name, boolean durable, boolean exclusive, boolean autoDelete,
         *                        @Nullable Map<String, Object> arguments)
         */
        Queue queue = new Queue("hello-java-queue", true, false, true);
        amqpAdmin.declareQueue(queue);
        log.info("Queue[{}]创建成功", "hello-java-queue");
    }

    @Test
    public void createBinding() {
        /**
         * Binding(String destination, DestinationType destinationType, String exchange, String routingKey,
         *                        @Nullable Map<String, Object> arguments)
         */
        Binding binding = new Binding("hello-java-queue", Binding.DestinationType.QUEUE, "hello-java-exchange", "hello.java", null);
        amqpAdmin.declareBinding(binding);
        log.info("Binding[{}]创建成功", "hello-java-binding");
    }

    @Test
    public void sendMessage() {

        // 发送消息，如果发送的消息是一个对象，会使用序列化机制，将对象写出去。要求对象必须实现Serializable接口
        // 发送的对象类型的消息，可以是一个json
        for (int i = 0; i < 10; i++) {
            if (i % 2 == 0) {
                OrderReturnReasonEntity orderReturnReasonEntity = new OrderReturnReasonEntity();
                orderReturnReasonEntity.setId(1L);
                orderReturnReasonEntity.setCreateTime(new Date());
                orderReturnReasonEntity.setName("哈哈" + i);
                rabbitTemplate.convertAndSend("hello-java-exchange", "hello.java", orderReturnReasonEntity);
            } else {
                OrderEntity orderEntity = new OrderEntity();
                orderEntity.setOrderSn(UUID.randomUUID().toString());
                rabbitTemplate.convertAndSend("hello-java-exchange", "hello.java", orderEntity);
            }
            log.info("消息发送完成");
        }

    }

}
