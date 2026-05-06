package org.example.ilink.message.service;

public interface MessageService {
    void receiveMessage();
    void sendMessage();

    /**
     * 发送提醒给指定用户
     * @param userId 用户ID
     * @param message 提醒内容
     */
    void sendReminderToUser(String userId, String message);
}
