package org.example.ilink.manager;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 微信登录管理器
 * 用于管理微信登录过程中的关键参数和状态
 */

@Component
@Data
@AllArgsConstructor
@NoArgsConstructor
public class WeChatLoginManager {
    // 轮询登录状态的令牌
    private String qrcode;
    // 后续请求的token令牌
    private String botToken;
    // 机器人id
    private String ilinkBotId;
    //    微信用户id
    private String ilinkUserId;
    //    基座返回地址
    private String baseUrl;
    // 登录状态 (wait:等待扫码, scaned:已扫码, confirmed:已确认, expired:已过期)
    private String status;

    /**
     * 设置登录成功信息
     */
    public void setLoginSuccess(String botToken, String ilinkBotId, String ilinkUserId, String baseUrl) {
        this.botToken = botToken;
        this.ilinkBotId = ilinkBotId;
        this.ilinkUserId = ilinkUserId;
        this.baseUrl = baseUrl;
        this.status = "confirmed";
    }
}