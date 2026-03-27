package org.example.ilink.service;

public interface MessageService {
    public void receiveMessage();
    public void sendMessage();

    /**
     * 发送提醒给指定用户
     * @param userId 用户ID
     * @param message 提醒内容
     */
    void sendReminderToUser(String userId, String message);
}
