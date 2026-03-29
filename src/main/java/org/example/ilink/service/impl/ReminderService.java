package org.example.ilink.service.impl;

import org.example.ilink.config.RabbitMQConfig;
import org.example.ilink.entity.message.ReminderMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
/**
 * 提醒服务
 * 负责发送延迟提醒消息到 RabbitMQ
 */
@Service
public class ReminderService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 创建延迟提醒
     * @param userId 用户ID
     * @param remind 提醒内容
     * @param delayMs 延迟毫秒数
     */
    public void createReminder(String userId, String remind, long delayMs) {
        if (delayMs <= 0) {
            System.out.println("延迟时间无效，不创建提醒");
            return;
        }

        ReminderMessage reminder = new ReminderMessage(userId, remind, delayMs);

        // 发送到延迟交换机，使用延迟消息插件
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.DELAY_EXCHANGE,
                RabbitMQConfig.REMINDER_ROUTING_KEY,
                reminder,
                message -> {
                    // 设置延迟时间（毫秒）
                    message.getMessageProperties().setHeader("x-delay", delayMs);
                    return message;
                }
        );

        System.out.println("已创建提醒: " + reminder + "，将在 " + (delayMs / 1000) + " 秒后触发");
    }

}
