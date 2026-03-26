package org.example.ilink.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.ilink.entity.message.MessageResponse;
import org.example.ilink.manager.MessageManager;
import org.example.ilink.manager.WeChatLoginManager;
import org.example.ilink.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class MessageServiceImpl implements MessageService {

    @Autowired
    private WeChatLoginManager weChatLoginManager;

    @Autowired
    private ObjectMapper objectMapper;  // 注入 ObjectMapper

    @Autowired
    private MessageManager messageManager;  // 注入 MessageManager

    private String getUpdatesBuf = "";
    // 存储接收到的消息参数
    private String receivedFromUserId;
    private final WebClient webClient;

    public MessageServiceImpl() {
        this.webClient = WebClient.builder()
                .baseUrl("https://ilinkai.weixin.qq.com")
                .build();
    }

    @Override
    public void receiveMessage() {
        // 检查登录状态
        if (!"confirmed".equals(weChatLoginManager.getStatus()) || weChatLoginManager.getBotToken() == null) {
            System.out.println("未登录，跳过消息接收");
            return;
        }

        // 生成随机uint32的十进制字符串并base64编码
        Random random = new Random();
        long uin = random.nextLong() & 0xFFFFFFFFL;
        String uinStr = String.valueOf(uin);
        String encodedUin = Base64.getEncoder().encodeToString(uinStr.getBytes());

        // 构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("get_updates_buf", getUpdatesBuf);

        Map<String, String> baseInfo = new HashMap<>();
        baseInfo.put("channel_version", "1.0.0");
        requestBody.put("base_info", baseInfo);

        System.out.println("开始长轮询，等待消息...");
        long startTime = System.currentTimeMillis();
        System.out.println("BotToken: " + weChatLoginManager.getBotToken());

        try {
            // 先获取原始二进制数据
            byte[] responseBytes = webClient.post()
                    .uri("/ilink/bot/getupdates")
                    .contentType(MediaType.APPLICATION_JSON)
                    // Bearer Token 格式：Bearer + 空格 + token
                    .header("Authorization", "Bearer " + weChatLoginManager.getBotToken())
                    .header("AuthorizationType", "ilink_bot_token")
                    .header("X-WECHAT-UIN", encodedUin)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .timeout(Duration.ofSeconds(60))
                    .block();

            long endTime = System.currentTimeMillis();
            System.out.println("长轮询返回，耗时: " + (endTime - startTime) + "ms");

            if (responseBytes != null) {
                // 将二进制数据转换为字符串
                String responseStr = new String(responseBytes);
                System.out.println("收到响应: " + responseStr);
                
                // 手动解析为MessageResponse
                MessageResponse response = objectMapper.readValue(responseStr, MessageResponse.class);
                
                // 处理响应
                handleResponse(response);
            }

        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("Read timed out")) {
                System.out.println("长轮询超时（60秒无消息），这是正常的，继续下一次轮询");
            } else {
                System.err.println("长轮询异常: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @Override
    public void sendMessage() {
        // 检查登录状态
        if (!"confirmed".equals(weChatLoginManager.getStatus()) || weChatLoginManager.getBotToken() == null) {
            System.out.println("未登录，跳过消息发送");
            return;
        }

        // 生成随机uint32的十进制字符串并base64编码
        Random random = new Random();
        long uin = random.nextLong() & 0xFFFFFFFFL;
        String uinStr = String.valueOf(uin);
        String encodedUin = Base64.getEncoder().encodeToString(uinStr.getBytes());

        // 构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        
        // 构建msg对象
        Map<String, Object> msg = new HashMap<>();
        msg.put("from_user_id", weChatLoginManager.getIlinkUserId());
        msg.put("to_user_id", messageManager.getIlinkUserId()); // 使用接收到的用户id
        msg.put("client_id", messageManager.getClientId()); // 使用MessageManager中的clientId
        msg.put("message_type", 2);
        msg.put("message_state", 2);
        msg.put("context_token", messageManager.getContextToken()); // 使用MessageManager中的contextToken
        
        // 构建item_list
        Map<String, Object> item = new HashMap<>();
        item.put("type", 1);
        
        Map<String, Object> textItem = new HashMap<>();
        textItem.put("text", "测试消息"); // 写死为测试消息
        item.put("text_item", textItem);
        
        java.util.List<Map<String, Object>> itemList = new java.util.ArrayList<>();
        itemList.add(item);
        msg.put("item_list", itemList);
        
        requestBody.put("msg", msg);
        
        // 构建base_info
        Map<String, String> baseInfo = new HashMap<>();
        baseInfo.put("channel_version", "1.0.0");
        requestBody.put("base_info", baseInfo);

        System.out.println("开始发送消息...");
        long startTime = System.currentTimeMillis();

        try {
            // 发送请求
            byte[] responseBytes = webClient.post()
                    .uri("/ilink/bot/sendmessage")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + weChatLoginManager.getBotToken())
                    .header("AuthorizationType", "ilink_bot_token")
                    .header("X-WECHAT-UIN", encodedUin)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();

            long endTime = System.currentTimeMillis();
            System.out.println("消息发送完成，耗时: " + (endTime - startTime) + "ms");

            if (responseBytes != null) {
                String responseStr = new String(responseBytes);
                System.out.println("发送消息响应: " + responseStr);
            }

        } catch (Exception e) {
            System.err.println("发送消息异常: " + e.getMessage());
            e.printStackTrace();
        }
    }



    private void handleResponse(MessageResponse response) {
        // 更新游标
        if (response.getGet_updates_buf() != null && !response.getGet_updates_buf().isEmpty()) {
            getUpdatesBuf = response.getGet_updates_buf();
            System.out.println("更新游标: " + getUpdatesBuf);
        }

        // 处理消息
        if (response.getMsgs() != null && !response.getMsgs().isEmpty()) {
            System.out.println("收到消息数量: " + response.getMsgs().size());
            // 处理消息
            handleMessages(response);
        } else {
            System.out.println("没有新消息");
        }
    }

    private void handleMessages(MessageResponse response) {
        // 实现具体的消息处理逻辑
        System.out.println("处理收到的消息");
        
        // 遍历所有消息
        for (org.example.ilink.entity.message.Message message : response.getMsgs()) {
            System.out.println("\n消息详情:");
            System.out.println("ilinkUserId: " + weChatLoginManager.getIlinkUserId());
            System.out.println("ilinkBotId: " + weChatLoginManager.getIlinkBotId());
            System.out.println("clientId: " + message.getClient_id());
            System.out.println("contextToken: " + message.getContext_token());
            
            // 存储接收到的消息参数到MessageManager
            receivedFromUserId = message.getFrom_user_id();
            messageManager.setIlinkUserId(weChatLoginManager.getIlinkUserId());
            messageManager.setIlinkBotId(weChatLoginManager.getIlinkBotId());
            messageManager.setClientId(message.getClient_id());
            messageManager.setContextToken(message.getContext_token());
            
            // 提取text内容
            if (message.getItem_list() != null) {
                for (org.example.ilink.entity.message.MessageItem item : message.getItem_list()) {
                    if (item.getText_item() != null) {
                        System.out.println("text: " + item.getText_item().getText());
                        sendMessage();
                    }
                }
            }
        }
    }
}