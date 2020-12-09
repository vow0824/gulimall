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
