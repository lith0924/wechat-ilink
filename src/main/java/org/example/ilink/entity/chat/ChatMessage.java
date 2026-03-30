package org.example.ilink.entity.chat;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户发送的消息记录
 */
@Data
@TableName("chat_message")
public class ChatMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 会话上下文 token */
    private String contextToken;

    /** 用户发送的内容 */
    private String content;

    /** 是否用到大模型 0=指令 1=AI对话 */
    private Integer useAi;

    /** 模型名称，useAi=0 时为空 */
    private String modelName;

    /** 聊天模式 default/knowledge/cosplay */
    private String chatMode;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
