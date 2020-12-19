package com.vow.gulimall.order.config;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
public class MyRabbitConfig {

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 定制RabbitTemplate
     * 1、服务器收到消息就回调
     *  1）、spring.rabbitmq.publisher-confirm-type=correlated
     *  2）、设置确认回调ConfirmCallback
     * 2、消息抵达队列进行回调
     *  1）、spring.rabbitmq.publisher-returns=true
     *       spring.rabbitmq.template.mandatory=true
     *  2）、设置确认回调ReturnCallback
     * 3、消费端确认（保证每一个消息被正确消费，此时broker才可以删除这个消息）
     *  1）、默认是自动确认的，只要消息接收到，客户端会自动确认，服务端就会移除这个消息
     *      问题：收到很多消息，自动回复给服务器ack，只有一个消息处理成功，客户端宕机后，
     *            服务器消息队列剩余消息均被移除队列，发生消息丢失
     *            消费者手动确认模式：只要我们没有明确告诉MQ，消息被消费，没有ack，消息就一直是unacked状态。即使Consumer
     *            宕机，MQ中的消息也不会丢失，会重新变为ready状态，下一次有Consumer连接进来就发给他。
     *  2）、如何签收：
     *      1、# 手动ack消息
     *         spring.rabbitmq.listener.simple.acknowledge-mode=manual
     *      2、channel.basicAck(deliveryTag, false);签收，业务成功完成
     *         channel.basicNack(deliveryTag, false, true);拒签，业务失败
     */
    @PostConstruct  // MyRabbitConfig对象创建完成以后调用这个方法
    public void initRabbitTemplate() {
        // 设置确认回调
        rabbitTemplate.setConfirmCallback(new RabbitTemplate.ConfirmCallback() {
            /**
             * 1、只要消息抵达broker，ack就为true
             * @param correlationData 当前消息的唯一关联数据（这个是消息的唯一id）
             * @param ack 消息是否成功收到
             * @param cause 失败的原因
             */
            @Override
            public void confirm(CorrelationData correlationData, boolean ack, String cause) {
                System.out.println("confirm...CorrelationData:[" + correlationData + "], ack[" + ack + "], cause[" + cause + "]");
            }
        });

        // 设置消息抵达队列的确认回调
        rabbitTemplate.setReturnCallback(new RabbitTemplate.ReturnCallback() {
            /**
             * 只要消息没有投递给指定的消息队列，就会触发这个失败回调
             * @param message 投递失败的消息的详细信息
             * @param replyCode 回复码
             * @param replyText 回复的内容
             * @param exchange  当时这个消息发给哪个交换器
             * @param routingKey 当时这个消息使用的路由键
             */
            @Override
            public void returnedMessage(Message message, int replyCode, String replyText, String exchange, String routingKey) {
                System.out.println("Fail Message[" + message + "], replayCode[" + replyCode + "], replayText[" + replyText + "], exchange[" + exchange + "], routingKey[" + routingKey + "]");
            }
        });
    }
}