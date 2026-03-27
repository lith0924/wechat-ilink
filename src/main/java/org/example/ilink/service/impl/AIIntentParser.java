package org.example.ilink.service.impl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.example.ilink.config.AIConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AIIntentParser {

    @Autowired
    private AIConfig aiConfig;

    private final Gson gson = new Gson();

    public ReminderInfo parseReminderIntent(String userMessage) {
        // 先用正则快速判断
        if (!isReminderRelated(userMessage)) {
            return null;
        }

        // 使用更严格的提示词，要求只返回 JSON
        String prompt = "你是一个提醒解析器。用户说：\"" + userMessage + "\"\n" +
                "请判断是否是提醒类消息。如果是，只返回以下JSON格式，不要有其他任何文字：\n" +
                "{\"is_reminder\": true, \"remind\": \"提醒内容\", \"delay_ms\": 延迟毫秒数}\n" +
                "如果不是提醒，只返回：{\"is_reminder\": false}\n\n" +
                "时间转换规则（计算从当前时间到提醒时间的毫秒数）：\n" +
                "- \"1分钟后\" -> delay_ms = 60000\n" +
                "- \"5分钟后\" -> delay_ms = 300000\n" +
                "- \"1小时后\" -> delay_ms = 3600000\n" +
                "- \"30秒后\" -> delay_ms = 30000\n" +
                "- \"明天上午9点\" -> 计算当前时间到明天9点的毫秒数\n\n" +
                "只返回JSON，不要有任何解释或其他文字。";

        try {
            String aiResponse = aiConfig.generateResponse(prompt);
            System.out.println("AI 原始返回: " + aiResponse);

            // 尝试提取 JSON
            String jsonStr = extractJSON(aiResponse);
            if (jsonStr == null) {
                System.out.println("无法提取 JSON，尝试正则解析");
                return parseByRegex(userMessage);
            }

            JsonObject json = gson.fromJson(jsonStr, JsonObject.class);

            if (!json.has("is_reminder") || !json.get("is_reminder").getAsBoolean()) {
                return null;
            }

            ReminderInfo info = new ReminderInfo();
            info.setReminder(true);

            if (json.has("remind") && !json.get("remind").isJsonNull()) {
                info.setRemind(json.get("remind").getAsString());
            }

            if (json.has("delay_ms") && !json.get("delay_ms").isJsonNull()) {
                info.setDelayMs(json.get("delay_ms").getAsLong());
            } else {
                // 如果 AI 没有返回 delay_ms，尝试从用户消息中解析
                info.setDelayMs(parseTimeFromMessage(userMessage));
            }

            return info;

        } catch (Exception e) {
            System.err.println("解析 AI 响应失败: " + e.getMessage());
            // 降级：使用正则解析
            return parseByRegex(userMessage);
        }
    }

    /**
     * 使用正则表达式直接解析用户消息（降级方案）
     */
    private ReminderInfo parseByRegex(String userMessage) {
        ReminderInfo info = new ReminderInfo();
        info.setReminder(true);

        // 提取时间
        Pattern timePattern = Pattern.compile("(\\d+)\\s*([分钟小时秒])");
        Matcher matcher = timePattern.matcher(userMessage);

        if (matcher.find()) {
            int number = Integer.parseInt(matcher.group(1));
            String unit = matcher.group(2);

            long delayMs = 0;
            switch (unit) {
                case "秒":
                    delayMs = number * 1000L;
                    break;
                case "分":
                    delayMs = number * 60 * 1000L;
                    break;
                case "小时":
                    delayMs = number * 60 * 60 * 1000L;
                    break;
            }
            info.setDelayMs(delayMs);
        }

        // 提取提醒内容
        Pattern remindPattern = Pattern.compile("提醒我?([^，,。.]*)");
        Matcher remindMatcher = remindPattern.matcher(userMessage);
        if (remindMatcher.find()) {
            String remind = remindMatcher.group(1).trim();
            if (remind.isEmpty()) {
                remind = "提醒事项";
            }
            info.setRemind(remind);
        } else {
            info.setRemind("提醒事项");
        }

        return info;
    }

    /**
     * 从用户消息中解析时间
     */
    private long parseTimeFromMessage(String message) {
        Pattern pattern = Pattern.compile("(\\d+)\\s*([分钟小时秒])");
        Matcher matcher = pattern.matcher(message);

        if (matcher.find()) {
            int number = Integer.parseInt(matcher.group(1));
            String unit = matcher.group(2);

            switch (unit) {
                case "秒":
                    return number * 1000L;
                case "分":
                    return number * 60 * 1000L;
                case "小时":
                    return number * 60 * 60 * 1000L;
            }
        }
        return 60000; // 默认1分钟
    }

    private boolean isReminderRelated(String message) {
        String[] keywords = {"提醒", "闹钟", "定时", "分钟", "小时", "秒", "后", "记得", "别忘"};
        for (String keyword : keywords) {
            if (message.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String extractJSON(String response) {
        // 尝试匹配 {...} 格式
        Pattern pattern = Pattern.compile("\\{[^{}]*\\}");
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    public static class ReminderInfo {
        private boolean isReminder;
        private String time;
        private String remind;
        private long delayMs;

        public boolean isReminder() { return isReminder; }
        public void setReminder(boolean reminder) { isReminder = reminder; }
        public String getTime() { return time; }
        public void setTime(String time) { this.time = time; }
        public String getRemind() { return remind; }
        public void setRemind(String remind) { this.remind = remind; }
        public long getDelayMs() { return delayMs; }
        public void setDelayMs(long delayMs) { this.delayMs = delayMs; }

        public String getReminderMessage() {
            if (remind != null && !remind.isEmpty()) {
                return remind;
            }
            return "该" + (time != null ? time : "提醒") + "啦！";
        }
    }
}