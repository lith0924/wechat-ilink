package org.example.ilink.entity.message;

import java.io.Serializable;

/**
 * 提醒消息实体
 */
public class ReminderMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private String userId;      // 用户ID
    private String remind;      // 提醒内容
    private long delay;         // 延迟时间(毫秒)
    private long createTime;    // 创建时间

    public ReminderMessage() {}

    public ReminderMessage(String userId, String remind, long delay) {
        this.userId = userId;
        this.remind = remind;
        this.delay = delay;
        this.createTime = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getRemind() { return remind; }
    public void setRemind(String remind) { this.remind = remind; }

    public long getDelay() { return delay; }
    public void setDelay(long delay) { this.delay = delay; }

    public long getCreateTime() { return createTime; }
    public void setCreateTime(long createTime) { this.createTime = createTime; }

    @Override
    public String toString() {
        return "ReminderMessage{" +
                "userId='" + userId + '\'' +
                ", remind='" + remind + '\'' +
                ", delay=" + delay +
                ", createTime=" + createTime +
                '}';
    }
}
