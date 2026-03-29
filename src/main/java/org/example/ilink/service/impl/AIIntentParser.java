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
            if (!isReminderRelated(userMessage)) {
                return null;
            }

            // 获取当前时间
            String currentTime = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss"));

            String prompt = "你是一个提醒解析器。\n" +
                    "当前时间：" + currentTime + "\n" +
                    "用户说：\"" + userMessage + "\"\n\n" +
                    "请判断是否是提醒类消息。如果是，只返回以下JSON格式，不要有任何其他文字：\n" +
                    "{\"is_reminder\": true, \"remind\": \"提醒内容\", \"delay_ms\": 延迟毫秒数}\n" +
                    "如果不是提醒，只返回：{\"is_reminder\": false}\n\n" +
                    "时间转换规则（计算从当前时间到提醒时间的毫秒数）：\n" +
                    "1. 相对时间：\n" +
                    "   - \"1分钟后\" -> delay_ms = 60000\n" +
                    "   - \"5分钟后\" -> delay_ms = 300000\n" +
                    "   - \"1小时后\" -> delay_ms = 3600000\n" +
                    "   - \"30秒后\" -> delay_ms = 30000\n" +
                    "2. 绝对时间（基于当前时间计算）：\n" +
                    "   - \"今天中午1点51分\" -> 计算到13:51:00的毫秒数\n" +
                    "   - \"今天下午2点半\" -> 计算到14:30:00的毫秒数\n" +
                    "   - \"明天上午9点\" -> 计算到明天09:00:00的毫秒数\n" +
                    "   - \"晚上9点10分\" -> 计算今天21:10:00的毫秒数，如果已过则计算明天\n" +
                    "   - \"9点10分\" -> 计算今天09:10:00的毫秒数，如果已过则计算明天\n" +
                    "3. 时间关键词映射：\n" +
                    "   - 中午12点 = 12:00，中午1点 = 13:00\n" +
                    "   - 下午1点 = 13:00，下午2点 = 14:00\n" +
                    "   - 晚上8点 = 20:00\n\n" +
                    "注意：delay_ms 必须是正整数，单位是毫秒。\n" +
                    "只返回JSON，不要有任何解释。";

            try {
                String aiResponse = aiConfig.generateResponse(prompt);
                System.out.println("AI 原始返回: " + aiResponse);

                String jsonStr = extractJSON(aiResponse);
                if (jsonStr == null) {
                    System.out.println("无法提取 JSON，尝试增强正则解析");
                    return parseByEnhancedRegex(userMessage);
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
                    // 如果AI没返回，使用增强的正则解析
                    info.setDelayMs(parseAbsoluteTime(userMessage));
                }

                return info;

            } catch (Exception e) {
                System.err.println("解析 AI 响应失败: " + e.getMessage());
                return parseByEnhancedRegex(userMessage);
            }
        }




    /**
     * 解析绝对时间
     */
    private long parseAbsoluteTime(String message) {
        // 匹配模式：今天中午1点51分、下午2点半、晚上9点等
        Pattern pattern = Pattern.compile(
                "(今天|明天)?\\s*(凌晨|早上|上午|中午|下午|晚上)?\\s*(\\d{1,2})\\s*点\\s*(\\d{1,2})?\\s*分?"
        );
        Matcher matcher = pattern.matcher(message);

        if (matcher.find()) {
            String dayFlag = matcher.group(1);     // 今天/明天
            String period = matcher.group(2);      // 上午/下午/中午等
            int hour = Integer.parseInt(matcher.group(3));
            int minute = matcher.group(4) != null ? Integer.parseInt(matcher.group(4)) : 0;

            // 根据时间段调整小时
            if (period != null) {
                switch (period) {
                    case "中午":
                        if (hour == 12) hour = 12;
                        else hour = hour + 12;  // 中午1点 -> 13点
                        break;
                    case "下午":
                        if (hour != 12) hour = hour + 12;
                        break;
                    case "晚上":
                        hour = hour + 12;
                        break;
                    case "凌晨":
                        if (hour == 12) hour = 0;
                        break;
                    // 上午和早上不需要调整
                }
            }

            // 如果只有时间没有时间段，且小时<=12，默认为上午
            if (period == null && hour <= 12) {
                // 保持原样，按24小时制
            }

            // 计算目标时间
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            java.time.LocalDateTime targetTime = java.time.LocalDateTime.of(
                    now.getYear(), now.getMonth(), now.getDayOfMonth(),
                    hour, minute, 0
            );

            // 处理"明天"或时间已过的情况
            if ("明天".equals(dayFlag) || targetTime.isBefore(now)) {
                targetTime = targetTime.plusDays(1);
            }

            // 计算毫秒差
            long delayMs = java.time.Duration.between(now, targetTime).toMillis();
            System.out.println("解析绝对时间: " + message + " -> " + delayMs + "ms (" + targetTime + ")");

            return delayMs;
        }

        return 0;
    }

    /**
     * 增强的正则解析（支持绝对时间）
     */
    private ReminderInfo parseByEnhancedRegex(String userMessage) {
        ReminderInfo info = new ReminderInfo();
        info.setReminder(true);

        // 先尝试解析绝对时间
        long delayMs = parseAbsoluteTime(userMessage);

        // 如果解析不到绝对时间，再尝试相对时间
        if (delayMs == 0) {
            delayMs = parseTimeFromMessage(userMessage);
        }

        info.setDelayMs(delayMs > 0 ? delayMs : 60000);  // 默认1分钟

        // 提取提醒内容
        Pattern remindPattern = Pattern.compile("提醒我?([^，,。.]*)");
        Matcher remindMatcher = remindPattern.matcher(userMessage);
        if (remindMatcher.find()) {
            String remind = remindMatcher.group(1).trim();
            // 移除时间部分
            remind = remind.replaceAll("(今天|明天)?\\s*(凌晨|早上|上午|中午|下午|晚上)?\\s*\\d+\\s*点\\s*\\d*\\s*分?", "");
            remind = remind.replaceAll("(\\d+)\\s*([分钟小时秒])", "");
            remind = remind.trim();
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