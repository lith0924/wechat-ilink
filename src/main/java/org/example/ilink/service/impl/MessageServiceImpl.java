package org.example.ilink.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.ilink.config.AIConfig;
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
    private ObjectMapper objectMapper;

    @Autowired
    private MessageManager messageManager;
    
    @Autowired
    private AIConfig aiConfig;


    @Autowired
    private AIIntentParser aiIntentParser;
    @Autowired
    private ReminderService reminderService;

    private String getUpdatesBuf = "";
    private String receivedFromUserId;
    private final WebClient webClient;

    public MessageServiceImpl() {
        this.webClient = WebClient.builder()
                .baseUrl("https://ilinkai.weixin.qq.com")
                .build();
    }

    @Override
    public void receiveMessage() {
        if (!"confirmed".equals(weChatLoginManager.getStatus()) || weChatLoginManager.getBotToken() == null) {
            System.out.println("未登录，跳过消息接收");
            return;
        }

        Random random = new Random();
        long uin = random.nextLong() & 0xFFFFFFFFL;
        String uinStr = String.valueOf(uin);
        String encodedUin = Base64.getEncoder().encodeToString(uinStr.getBytes());

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("get_updates_buf", getUpdatesBuf);

        Map<String, String> baseInfo = new HashMap<>();
        baseInfo.put("channel_version", "1.0.0");
        requestBody.put("base_info", baseInfo);

        System.out.println("开始长轮询，等待消息...");
        long startTime = System.currentTimeMillis();
        System.out.println("BotToken: " + weChatLoginManager.getBotToken());

        try {
            byte[] responseBytes = webClient.post()
                    .uri("/ilink/bot/getupdates")
                    .contentType(MediaType.APPLICATION_JSON)
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
                String responseStr = new String(responseBytes);
                System.out.println("收到响应: " + responseStr);
                
                MessageResponse response = objectMapper.readValue(responseStr, MessageResponse.class);
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
        if (!"confirmed".equals(weChatLoginManager.getStatus()) || weChatLoginManager.getBotToken() == null) {
            System.out.println("未登录，跳过消息发送");
            return;
        }

        Random random = new Random();
        long uin = random.nextLong() & 0xFFFFFFFFL;
        String uinStr = String.valueOf(uin);
        String encodedUin = Base64.getEncoder().encodeToString(uinStr.getBytes());

        Map<String, Object> requestBody = new HashMap<>();

        Map<String, Object> msg = new HashMap<>();
        msg.put("from_user_id", weChatLoginManager.getIlinkUserId());
        msg.put("to_user_id", messageManager.getIlinkUserId());
        msg.put("client_id", messageManager.getClientId());
        msg.put("message_type", 2);
        msg.put("message_state", 2);
        msg.put("context_token", messageManager.getContextToken());

        Map<String, Object> item = new HashMap<>();
        item.put("type", 1);

        Map<String, Object> textItem = new HashMap<>();
        textItem.put("text", "测试消息");
        item.put("text_item", textItem);

        java.util.List<Map<String, Object>> itemList = new java.util.ArrayList<>();
        itemList.add(item);
        msg.put("item_list", itemList);

        requestBody.put("msg", msg);

        Map<String, String> baseInfo = new HashMap<>();
        baseInfo.put("channel_version", "1.0.0");
        requestBody.put("base_info", baseInfo);

        System.out.println("开始发送消息...");
        long startTime = System.currentTimeMillis();

        try {
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
        if (response.getGet_updates_buf() != null && !response.getGet_updates_buf().isEmpty()) {
            getUpdatesBuf = response.getGet_updates_buf();
            System.out.println("更新游标: " + getUpdatesBuf);
        }

        if (response.getMsgs() != null && !response.getMsgs().isEmpty()) {
            System.out.println("收到消息数量: " + response.getMsgs().size());
            handleMessages(response);
        } else {
            System.out.println("没有新消息");
        }
    }

    private void handleMessages(MessageResponse response) {
        System.out.println("处理收到的消息");
        
        for (org.example.ilink.entity.message.Message message : response.getMsgs()) {
            System.out.println("\n消息详情:");
            System.out.println("ilinkUserId: " + weChatLoginManager.getIlinkUserId());
            System.out.println("ilinkBotId: " + weChatLoginManager.getIlinkBotId());
            System.out.println("clientId: " + message.getClient_id());
            System.out.println("contextToken: " + message.getContext_token());
            
            receivedFromUserId = message.getFrom_user_id();
            messageManager.setIlinkUserId(weChatLoginManager.getIlinkUserId());
            messageManager.setIlinkBotId(weChatLoginManager.getIlinkBotId());
            messageManager.setClientId(message.getClient_id());
            messageManager.setContextToken(message.getContext_token());
            
            if (message.getItem_list() != null) {
                for (org.example.ilink.entity.message.MessageItem item : message.getItem_list()) {
                    if (item.getText_item() != null) {
                        String userMessage = item.getText_item().getText();
                        System.out.println("用户消息: " + userMessage);
                        
//                        // 调用 AI 生成回复
//                        String aiResponse = aiConfig.generateResponse(userMessage);
//                        System.out.println("AI 回复: " + aiResponse);
//
//                        // 发送 AI 回复
//                        sendAIMessage(aiResponse);

                        // 先尝试解析提醒意图
                        AIIntentParser.ReminderInfo reminderInfo = aiIntentParser.parseReminderIntent(userMessage);

                        if (reminderInfo != null && reminderInfo.isReminder()) {
                            // 是提醒意图，创建延迟提醒
                            String remindContent = reminderInfo.getReminderMessage();
                            long delayMs = reminderInfo.getDelayMs();

                            if (delayMs > 0) {
                                reminderService.createReminder(
                                        receivedFromUserId,
                                        remindContent,
                                        delayMs
                                );

                                // 发送确认消息
                                String confirmMsg = "✅ 已设置提醒：" + remindContent +
                                        "，将在 " + (delayMs / 1000) + " 秒后提醒您";
//                                sendAIMessage(confirmMsg);
                                sendReminderToUser(receivedFromUserId, confirmMsg);
                            } else {
                                sendAIMessage("抱歉，我没能理解提醒时间，请说「3分钟后提醒我喝水」这样的格式");
                            }
                        } else {
                            // 不是提醒，正常调用 AI 对话
                            String aiResponse = aiConfig.generateResponse(userMessage);
                            System.out.println("AI 回复: " + aiResponse);
                            sendAIMessage(aiResponse);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 发送 AI 生成的消息
     */
    private void sendAIMessage(String aiMessage) {
        if (!"confirmed".equals(weChatLoginManager.getStatus()) || weChatLoginManager.getBotToken() == null) {
            System.out.println("未登录，跳过消息发送");
            return;
        }

        Random random = new Random();
        long uin = random.nextLong() & 0xFFFFFFFFL;
        String uinStr = String.valueOf(uin);
        String encodedUin = Base64.getEncoder().encodeToString(uinStr.getBytes());

        Map<String, Object> requestBody = new HashMap<>();
        
        Map<String, Object> msg = new HashMap<>();
        msg.put("from_user_id", weChatLoginManager.getIlinkUserId());
        msg.put("to_user_id", messageManager.getIlinkUserId());
        msg.put("client_id", messageManager.getClientId());
        msg.put("message_type", 2);
        msg.put("message_state", 2);
        msg.put("context_token", messageManager.getContextToken());
        
        Map<String, Object> item = new HashMap<>();
        item.put("type", 1);
        
        Map<String, Object> textItem = new HashMap<>();
        textItem.put("text", aiMessage);  // 使用 AI 生成的消息
        item.put("text_item", textItem);
        
        java.util.List<Map<String, Object>> itemList = new java.util.ArrayList<>();
        itemList.add(item);
        msg.put("item_list", itemList);
        
        requestBody.put("msg", msg);
        
        Map<String, String> baseInfo = new HashMap<>();
        baseInfo.put("channel_version", "1.0.0");
        requestBody.put("base_info", baseInfo);

        System.out.println("发送 AI 消息...");

        try {
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

            if (responseBytes != null) {
                String responseStr = new String(responseBytes);
                System.out.println("AI 消息发送成功: " + responseStr);
            }

        } catch (Exception e) {
            System.err.println("发送 AI 消息异常: " + e.getMessage());
            e.printStackTrace();
        }
    }



    @Override
    public void sendReminderToUser(String userId, String message) {
        if (!"confirmed".equals(weChatLoginManager.getStatus()) || weChatLoginManager.getBotToken() == null) {
            System.out.println("未登录，跳过消息发送");
            return;
        }

        Random random = new Random();
        long uin = random.nextLong() & 0xFFFFFFFFL;
        String uinStr = String.valueOf(uin);
        String encodedUin = Base64.getEncoder().encodeToString(uinStr.getBytes());

        Map<String, Object> requestBody = new HashMap<>();

        Map<String, Object> msg = new HashMap<>();
        msg.put("from_user_id", weChatLoginManager.getIlinkUserId());
        msg.put("to_user_id", userId);  // 发送给指定用户
//        msg.put("client_id", messageManager.getClientId());
        msg.put("client_id", "reminder_" + System.currentTimeMillis() + "_" + random.nextInt(1000));
        msg.put("message_type", 2);
        msg.put("message_state", 2);
        msg.put("context_token", messageManager.getContextToken());

        Map<String, Object> item = new HashMap<>();
        item.put("type", 1);

        Map<String, Object> textItem = new HashMap<>();
        textItem.put("text", message);
        item.put("text_item", textItem);

        java.util.List<Map<String, Object>> itemList = new java.util.ArrayList<>();
        itemList.add(item);
        msg.put("item_list", itemList);

        requestBody.put("msg", msg);

        Map<String, String> baseInfo = new HashMap<>();
        baseInfo.put("channel_version", "1.0.0");
        requestBody.put("base_info", baseInfo);

        System.out.println("发送提醒消息: " + message);

        try {
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

            if (responseBytes != null) {
                String responseStr = new String(responseBytes);
                System.out.println("提醒消息发送成功: " + responseStr);
            }

        } catch (Exception e) {
            System.err.println("发送提醒消息异常: " + e.getMessage());
            e.printStackTrace();
        }
    }


}
