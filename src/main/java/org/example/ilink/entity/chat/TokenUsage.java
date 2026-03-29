package org.example.ilink.entity.chat;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Token 消耗记录实体
 */
@Data
@TableName("token_usage")
public class TokenUsage {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 会话 ID */
    private String sessionId;

    /** 微信用户 ID */
    private String userId;

    /** 使用的 AI 模型 */
    private String modelName;

    /** 输入 Token 数 */
    private Integer promptTokens;

    /** 输出 Token 数 */
    private Integer completionTokens;

    /** 总 Token 数 */
    private Integer totalTokens;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
