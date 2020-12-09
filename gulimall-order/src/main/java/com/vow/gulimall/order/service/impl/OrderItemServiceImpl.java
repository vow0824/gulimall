package com.vow.gulimall.order.service.impl;

import com.rabbitmq.client.Channel;
import com.vow.gulimall.order.entity.OrderEntity;
import com.vow.gulimall.order.entity.OrderReturnReasonEntity;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vow.common.utils.PageUtils;
import com.vow.common.utils.Query;

import com.vow.gulimall.order.dao.OrderItemDao;
import com.vow.gulimall.order.entity.OrderItemEntity;
import com.vow.gulimall.order.service.OrderItemService;

@RabbitListener(queues = {"hello-java-queue"})
@Service("orderItemService")
public class OrderItemServiceImpl extends ServiceImpl<OrderItemDao, OrderItemEntity> implements OrderItemService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderItemEntity> page = this.page(
                new Query<OrderItemEntity>().getPage(params),
                new QueryWrapper<OrderItemEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * queues：声明所有需要监听的队列
     * org.springframework.amqp.core.Message
     * 参数可以写以下类型
     * 1、Message message：原生消息详细信息，头+体
     * 2、T<发送的消息的类型>
     * 3、Channel channel：当前传输数据的通道
     *
     * Queue：可以有很多人都来监听。只要收到消息，队列就会删除消息，而且只能有一个收到此消息
     * 场景：
     *  1）、订单服务启动多个：同一个消息只能有一个客户端收到
     *  2）、只有一个消息完全处理完，方法运行结束，就可以接收到下一个消息
     */
    /*@RabbitListener(queues = {"hello-java-queue"})
    public void receiveMessage(Message message, OrderReturnReasonEntity content, Channel channel) throws InterruptedException {
        System.out.println("接收到消息。。。" + content);
        // 消息体
        // Body:'{"id":1,"name":"哈哈","sort":null,"status":null,"createTime":1607477280736}'
        byte[] body = message.getBody();
        // 消息头属性信息
        // MessageProperties [headers={__TypeId__=com.vow.gulimall.order.entity.OrderReturnReasonEntity},
        // contentType=application/json, contentEncoding=UTF-8, contentLength=0, receivedDeliveryMode=PERSISTENT,
        // priority=0, redelivered=false, receivedExchange=hello-java-exchange, receivedRoutingKey=hello.java,
        // deliveryTag=1, consumerTag=amq.ctag-_QWfovW0CSk-2F2mSDTreQ, consumerQueue=hello-java-queue])
        MessageProperties messageProperties = message.getMessageProperties();
        Thread.sleep(3000);
        System.out.println("消息处理完成：" + content.getName());
    }*/

    @RabbitHandler
    public void receiveMessage(Message message, OrderReturnReasonEntity content, Channel channel) throws InterruptedException {
        System.out.println("接收到消息。。。" + content);
        byte[] body = message.getBody();
        MessageProperties messageProperties = message.getMessageProperties();
        System.out.println("消息处理完成：" + content.getName());
    }

    @RabbitHandler
    public void receiveMessage2(OrderEntity content) throws InterruptedException {
        System.out.println("接收到消息。。。" + content);
    }

}