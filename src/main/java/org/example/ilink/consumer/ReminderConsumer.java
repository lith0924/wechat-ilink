package org.example.ilink.consumer;

import org.example.ilink.config.RabbitMQConfig;
import org.example.ilink.entity.message.ReminderMessage;
import org.example.ilink.service.MessageService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 提醒消息消费者（确认提醒设置）
 * 接收过期提醒并发送给用户
 */
@Component
public class ReminderConsumer {

    @Autowired
    private MessageService messageService;

    /**
     * 监听提醒队列，处理到期的提醒
     */
    @RabbitListener(queues = RabbitMQConfig.REMINDER_QUEUE, concurrency = "5")
    public void handleReminder(ReminderMessage reminder) {
        System.out.println("收到提醒消息: " + reminder);

        // 构建提醒文本
        String reminderText = buildReminderText(reminder);

        // 发送提醒消息给用户
        messageService.sendReminderToUser(reminder.getUserId(), reminderText);

        System.out.println("提醒已发送: " + reminderText);
    }

    /**
     * 构建提醒文本
     */
    private String buildReminderText(ReminderMessage reminder) {
        String remind = reminder.getRemind();
        if (remind != null && !remind.isEmpty()) {
            return "⏰ 提醒：" + remind + "！";
        }
        return "⏰ 时间到啦！该休息一下了~";
    }
}