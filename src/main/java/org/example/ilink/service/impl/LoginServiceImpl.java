package org.example.ilink.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.ilink.manager.WeChatLoginManager;
import org.example.ilink.service.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class LoginServiceImpl implements LoginService {
    @Autowired
    private WeChatLoginManager weChatLoginManager;
    private static final String QRCODE_URL = "https://ilinkai.weixin.qq.com/ilink/bot/get_bot_qrcode?bot_type=3";
    private static final String QRCODE_STATUS_URL = "https://ilinkai.weixin.qq.com/ilink/bot/get_qrcode_status";
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public LoginServiceImpl() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String loginWeChat() {
        try {
            // 构建GET请求
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(QRCODE_URL))
                    .timeout(Duration.ofSeconds(30))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            // 发送请求
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            // 检查响应状态码
            if (response.statusCode() != 200) {
                throw new RuntimeException("请求失败，状态码: " + response.statusCode());
            }

            // 解析响应JSON
            String responseBody = response.body();
            JsonNode jsonNode = objectMapper.readTree(responseBody);

            // 检查ret返回码
            int ret = jsonNode.get("ret").asInt();
            if (ret != 0) {
                throw new RuntimeException("接口返回错误，ret: " + ret);
            }

            // 获取qrcode和qrcode_img_content
            String qrcode = jsonNode.get("qrcode").asText();
            String qrcodeImgContent = jsonNode.get("qrcode_img_content").asText();

            // 将qrcode存储到静态变量
            weChatLoginManager.setQrcode(qrcode);

            // 返回qrcode_img_content
            return qrcodeImgContent;
        } catch (Exception e) {
            throw new RuntimeException("获取微信登录二维码失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String loginStatus() {
        try {
            // 检查是否有qrcode
            String qrcode = weChatLoginManager.getQrcode();
            if (qrcode == null || qrcode.isEmpty()) {
                throw new RuntimeException("未获取到二维码");
            }

            // 构建请求URL
            String url = QRCODE_STATUS_URL + "?qrcode=" + qrcode;

            // 构建GET请求，携带请求头
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .header("Accept", "application/json")
                    .header("iLink-App-ClientVersion", "1")
                    .GET()
                    .build();

            // 发送请求
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            // 检查响应状态码
            if (response.statusCode() != 200) {
                throw new RuntimeException("查询登录状态失败，状态码: " + response.statusCode());
            }

            // 解析响应JSON
            String responseBody = response.body();
            JsonNode jsonNode = objectMapper.readTree(responseBody);

            // 获取status字段
            String status = jsonNode.get("status").asText();

            // 更新manager中的状态
            weChatLoginManager.setStatus(status);

            // 如果状态是confirmed（已确认），则保存返回的其他信息
            if ("confirmed".equals(status)) {
                if (jsonNode.has("bot_token")) {
                    String botToken = jsonNode.get("bot_token").asText();
                    weChatLoginManager.setBotToken(botToken);
                }
                if (jsonNode.has("ilink_bot_id")) {
                    String ilinkBotId = jsonNode.get("ilink_bot_id").asText();
                    weChatLoginManager.setIlinkBotId(ilinkBotId);
                }
                if (jsonNode.has("ilink_user_id")) {
                    String ilinkUserId = jsonNode.get("ilink_user_id").asText();
                    weChatLoginManager.setIlinkUserId(ilinkUserId);
                }
                if (jsonNode.has("baseurl")) {
                    String baseUrl = jsonNode.get("baseurl").asText();
                    weChatLoginManager.setBaseUrl(baseUrl);
                }
            }
            // 返回status
            return status;
        } catch (Exception e) {
            throw new RuntimeException("查询微信登录状态失败: " + e.getMessage(), e);
        }
    }
}