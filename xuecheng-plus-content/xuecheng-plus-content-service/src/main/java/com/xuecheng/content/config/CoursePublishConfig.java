package com.xuecheng.content.config;

import com.alibaba.fastjson.JSON;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MqMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Mr.M
 * @version 1.0
 * @description TODO
 * @date 2023/2/23 16:59
 */
@Slf4j
@Configuration
public class CoursePublishConfig implements ApplicationContextAware {

    //交换机
    public static final String COURSEPUBLISH_EXCHANGE_FANOUT = "coursePublish_exchange_fanout";
    //课程发布消息类型
    public static final String MESSAGE_TYPE = "course_publish";
    //课程发布队列
    public static final String COURSEPUBLISH_QUEUE = "coursePublish_queue";

    //声明交换机，且持久化
    @Bean(COURSEPUBLISH_EXCHANGE_FANOUT)
    public FanoutExchange paynotify_exchange_fanout() {
        // 三个参数：交换机名称、是否持久化、当没有queue与其绑定时是否自动删除
        return new FanoutExchange(COURSEPUBLISH_EXCHANGE_FANOUT, true, false);
    }
    //课程发布队列,且持久化
    @Bean(COURSEPUBLISH_QUEUE)
    public Queue course_publish_queue() {
        return QueueBuilder.durable(COURSEPUBLISH_QUEUE).build();
    }

    //交换机和课程发布队列绑定
    @Bean
    public Binding binding_course_publish_queue(@Qualifier(COURSEPUBLISH_QUEUE) Queue queue, @Qualifier(COURSEPUBLISH_EXCHANGE_FANOUT) FanoutExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        // 获取RabbitTemplate
        RabbitTemplate rabbitTemplate = applicationContext.getBean(RabbitTemplate.class);
        //消息处理service
        MqMessageService mqMessageService = applicationContext.getBean(MqMessageService.class);
        // 设置ReturnCallback
        rabbitTemplate.setReturnCallback((message, replyCode, replyText, exchange, routingKey) -> {
            // 投递失败，记录日志
            log.info("消息发送失败，应答码{}，原因{}，交换机{}，路由键{},消息{}",
                    replyCode, replyText, exchange, routingKey, message);
            MqMessage mqMessage = JSON.parseObject(message.toString(), MqMessage.class);
            //将消息再添加到消息表
            mqMessageService.addMessage(mqMessage.getMessageType(),mqMessage.getBusinessKey1(),mqMessage.getBusinessKey2(),mqMessage.getBusinessKey3());

        });
    }
}
