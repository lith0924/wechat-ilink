package org.example.ilink.entity.chat;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Bot 回复记录
 */
@Data
@TableName("chat_reply")
public class ChatReply {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联 chat_message.id */
    private Long messageId;

    /** 回复内容 */
    private String content;

    /** 消耗 token 总数 */
    private Integer totalTokens;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
