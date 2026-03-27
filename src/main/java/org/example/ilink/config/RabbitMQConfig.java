package org.example.ilink.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置
 */
@Configuration
public class RabbitMQConfig {

    // 交换机名称
    public static final String REMINDER_EXCHANGE = "reminder.exchange";

    // 队列名称
    public static final String REMINDER_QUEUE = "reminder.queue";

    // 路由键
    public static final String REMINDER_ROUTING_KEY = "reminder.key";

    // 延迟队列（使用死信队列实现延迟）
    public static final String DELAY_QUEUE = "delay.queue";
    public static final String DELAY_EXCHANGE = "delay.exchange";
    public static final String DELAY_ROUTING_KEY = "delay.key";

    /**
     * 消息转换器，使用JSON格式
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 配置 RabbitTemplate
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }

    /**
     * 延迟队列（用于延迟消息）
     * 该队列的消息过期后会转发到死信交换机和死信队列
     */
    @Bean
    public Queue delayQueue() {
        return QueueBuilder.durable(DELAY_QUEUE)
                .withArgument("x-dead-letter-exchange", REMINDER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", REMINDER_ROUTING_KEY)
                .build();
    }

    /**
     * 延迟交换机
     */
    @Bean
    public DirectExchange delayExchange() {
        return new DirectExchange(DELAY_EXCHANGE);
    }

    /**
     * 绑定延迟队列到延迟交换机
     */
    @Bean
    public Binding delayBinding() {
        return BindingBuilder.bind(delayQueue())
                .to(delayExchange())
                .with(DELAY_ROUTING_KEY);
    }

    /**
     * 实际处理提醒的队列
     */
    @Bean
    public Queue reminderQueue() {
        return QueueBuilder.durable(REMINDER_QUEUE).build();
    }

    /**
     * 提醒交换机
     */
    @Bean
    public DirectExchange reminderExchange() {
        return new DirectExchange(REMINDER_EXCHANGE);
    }

    /**
     * 绑定提醒队列到提醒交换机
     */
    @Bean
    public Binding reminderBinding() {
        return BindingBuilder.bind(reminderQueue())
                .to(reminderExchange())
                .with(REMINDER_ROUTING_KEY);
    }
}
