package org.example.ilink.entity.chat;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天记录实体
 */
@Data
@TableName("chat_record")
public class ChatRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 会话 ID（同一个用户的连续对话） */
    private String sessionId;

    /** 微信用户 ID */
    private String userId;

    /** 用户发的就是user，ai回复的就是assistant **/
    private String role;

    /** 消息内容 */
    private String content;

    /** 使用的 AI 模型 */
    private String modelName;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
