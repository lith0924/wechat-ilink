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

        // 发送到延迟队列，设置消息过期时间
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.DELAY_EXCHANGE,
                RabbitMQConfig.DELAY_ROUTING_KEY,
                reminder,
                message -> {
                    // 设置消息过期时间（毫秒）
                    message.getMessageProperties().setExpiration(String.valueOf(delayMs));
                    return message;
                }
        );

        System.out.println("已创建提醒: " + reminder + "，将在 " + (delayMs / 1000) + " 秒后触发");
    }

    /**
     * 创建延迟提醒（使用时间描述）
     * @param userId 用户ID
     * @param remind 提醒内容
     * @param timeDesc 时间描述（如"3分钟后"）
     */
    public void createReminder(String userId, String remind, String timeDesc) {
        long delayMs = parseTimeToDelay(timeDesc);
        createReminder(userId, remind, delayMs);
    }

    /**
     * 解析时间描述为延迟毫秒数
     */
    private long parseTimeToDelay(String timeDesc) {
        if (timeDesc == null) return 0;

        try {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)\\s*([分钟小时秒天后])");
            java.util.regex.Matcher matcher = pattern.matcher(timeDesc);

            if (matcher.find()) {
                int number = Integer.parseInt(matcher.group(1));
                String unit = matcher.group(2);

                switch (unit) {
                    case "秒":
                        return number * 1000L;
                    case "分":
                        return number * 60 * 1000L;
                    case "小时":
                        return number * 60 * 60 * 1000L;
                    case "天":
                        return number * 24 * 60 * 60 * 1000L;
                }
            }
        } catch (Exception e) {
            System.err.println("解析时间失败: " + e.getMessage());
        }

        return 0;
    }
}
