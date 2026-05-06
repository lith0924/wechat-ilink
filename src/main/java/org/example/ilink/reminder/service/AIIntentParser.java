package org.example.ilink.reminder.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.example.ilink.config.AIConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AIIntentParser {

    private static final Pattern ABSOLUTE_TIME_PATTERN = Pattern.compile(
            "(今天|明天)?\\s*(凌晨|早上|上午|中午|下午|晚上)?\\s*(\\d{1,2})\\s*点\\s*(\\d{1,2})?\\s*分?"
    );
    private static final Pattern DIGIT_RELATIVE_PATTERN = Pattern.compile(
            "(\\d+)\\s*(秒钟?|分钟?|小时)\\s*(内|后|之后|以后)?"
    );
    private static final Pattern CHINESE_RELATIVE_PATTERN = Pattern.compile(
            "([零一二两三四五六七八九十百]+)\\s*(秒钟?|分钟?|小时)\\s*(内|后|之后|以后)?"
    );
    private static final Pattern PREFIX_DIGIT_RELATIVE_PATTERN = Pattern.compile(
            "过\\s*(\\d+)\\s*(秒钟?|分钟?|小时)"
    );
    private static final Pattern PREFIX_CHINESE_RELATIVE_PATTERN = Pattern.compile(
            "过\\s*([零一二两三四五六七八九十百]+)\\s*(秒钟?|分钟?|小时)"
    );
    private static final Pattern HALF_HOUR_PATTERN = Pattern.compile("半小时\\s*(内|后|之后|以后)?");
    private static final Pattern A_WHILE_PATTERN = Pattern.compile("(一会儿?|一小会儿?|等会儿?|等会|待会儿?|待会|晚点)");
    private static final Pattern RELATIVE_TIME_EXPRESSION_PATTERN = Pattern.compile(
            "(过\\s*(\\d+|[零一二两三四五六七八九十百]+)\\s*(秒钟?|分钟?|小时)|((\\d+|[零一二两三四五六七八九十百]+)\\s*(秒钟?|分钟?|小时)|半小时|一会儿?|一小会儿?|等会儿?|等会|待会儿?|待会|晚点)\\s*(内|后|之后|以后)?)"
    );

    @Autowired
    private AIConfig aiConfig;

    private final Gson gson = new Gson();

    public ReminderInfo parseReminderIntent(String userMessage) {
        if (!isReminderRelated(userMessage)) {
            return null;
        }

        if (hasRelativeTimeExpression(userMessage)) {
            return parseByEnhancedRegex(userMessage);
        }

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
                "   - \"9点10分\" -> 计算今天09:10:00的毫秒数，如果已过则计算明天\n\n" +
                "注意：delay_ms 必须是正整数，单位是毫秒。\n" +
                "只返回JSON，不要有任何解释。";

        try {
            String aiResponse = aiConfig.generateResponse(prompt);
            System.out.println("AI 原始返回: " + aiResponse);

            String jsonStr = extractJSON(aiResponse);
            if (jsonStr == null) {
                System.out.println("无法提取 JSON，当前消息不按提醒处理");
                return null;
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
                info.setDelayMs(parseAbsoluteTime(userMessage));
            }

            if (info.getDelayMs() <= 0) {
                info.setDelayMs(parseTimeFromMessage(userMessage));
            }
            if (info.getRemind() == null || info.getRemind().isBlank()) {
                info.setRemind(extractReminderContent(userMessage));
            }

            return info;
        } catch (Exception e) {
            System.err.println("解析 AI 响应失败: " + e.getMessage());
            return null;
        }
    }

    private long parseAbsoluteTime(String message) {
        Matcher matcher = ABSOLUTE_TIME_PATTERN.matcher(message);
        if (!matcher.find()) {
            return 0;
        }

        String dayFlag = matcher.group(1);
        String period = matcher.group(2);
        int hour = Integer.parseInt(matcher.group(3));
        int minute = matcher.group(4) != null ? Integer.parseInt(matcher.group(4)) : 0;

        if (period != null) {
            switch (period) {
                case "中午":
                case "下午":
                    if (hour != 12) hour += 12;
                    break;
                case "晚上":
                    hour += 12;
                    break;
                case "凌晨":
                    if (hour == 12) hour = 0;
                    break;
                default:
                    break;
            }
        }

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime targetTime = java.time.LocalDateTime.of(
                now.getYear(), now.getMonth(), now.getDayOfMonth(), hour, minute, 0
        );

        if ("明天".equals(dayFlag) || targetTime.isBefore(now)) {
            targetTime = targetTime.plusDays(1);
        }

        long delayMs = java.time.Duration.between(now, targetTime).toMillis();
        System.out.println("解析绝对时间: " + message + " -> " + delayMs + "ms (" + targetTime + ")");
        return delayMs;
    }

    private ReminderInfo parseByEnhancedRegex(String userMessage) {
        ReminderInfo info = new ReminderInfo();
        info.setReminder(true);

        long delayMs = parseAbsoluteTime(userMessage);
        if (delayMs == 0) {
            delayMs = parseTimeFromMessage(userMessage);
        }

        info.setDelayMs(delayMs > 0 ? delayMs : 60000);
        info.setRemind(extractReminderContent(userMessage));
        return info;
    }

    private long parseTimeFromMessage(String message) {
        Matcher prefixDigitMatcher = PREFIX_DIGIT_RELATIVE_PATTERN.matcher(message);
        if (prefixDigitMatcher.find()) {
            return toDelayMs(Integer.parseInt(prefixDigitMatcher.group(1)), normalizeTimeUnit(prefixDigitMatcher.group(2)));
        }

        Matcher prefixChineseMatcher = PREFIX_CHINESE_RELATIVE_PATTERN.matcher(message);
        if (prefixChineseMatcher.find()) {
            return toDelayMs(parseChineseNumber(prefixChineseMatcher.group(1)), normalizeTimeUnit(prefixChineseMatcher.group(2)));
        }

        Matcher halfHourMatcher = HALF_HOUR_PATTERN.matcher(message);
        if (halfHourMatcher.find()) {
            return 30 * 60 * 1000L;
        }

        Matcher aWhileMatcher = A_WHILE_PATTERN.matcher(message);
        if (aWhileMatcher.find()) {
            return 5 * 60 * 1000L;
        }

        Matcher digitMatcher = DIGIT_RELATIVE_PATTERN.matcher(message);
        if (digitMatcher.find()) {
            return toDelayMs(Integer.parseInt(digitMatcher.group(1)), normalizeTimeUnit(digitMatcher.group(2)));
        }

        Matcher chineseMatcher = CHINESE_RELATIVE_PATTERN.matcher(message);
        if (chineseMatcher.find()) {
            return toDelayMs(parseChineseNumber(chineseMatcher.group(1)), normalizeTimeUnit(chineseMatcher.group(2)));
        }

        return 60000L;
    }

    private boolean hasRelativeTimeExpression(String message) {
        return RELATIVE_TIME_EXPRESSION_PATTERN.matcher(message).find();
    }

    private String extractReminderContent(String userMessage) {
        String remind = userMessage;
        remind = RELATIVE_TIME_EXPRESSION_PATTERN.matcher(remind).replaceAll("");
        remind = ABSOLUTE_TIME_PATTERN.matcher(remind).replaceAll("");
        remind = remind.replaceFirst("^提醒我", "");
        remind = remind.replaceFirst("^提醒", "");
        remind = remind.replaceFirst("^记得", "");
        remind = remind.replaceFirst("^别忘了?", "");
        remind = remind.replaceFirst("提醒我", "");
        remind = remind.replaceFirst("提醒", "");
        remind = remind.replaceFirst("记得", "");
        remind = remind.replaceFirst("别忘了?", "");
        remind = remind.replaceAll("^[，,。.!！?？的地得\s]+|[，,。.!！?？\s]+$", "");
        if (remind.isBlank()) {
            return "提醒事项";
        }
        return remind;
    }

    private long toDelayMs(int number, String unit) {
        switch (unit) {
            case "秒":
                return number * 1000L;
            case "分":
                return number * 60 * 1000L;
            case "小时":
                return number * 60 * 60 * 1000L;
            default:
                return 60000L;
        }
    }

    private String normalizeTimeUnit(String unit) {
        if (unit.startsWith("秒")) {
            return "秒";
        }
        if (unit.startsWith("分")) {
            return "分";
        }
        return "小时";
    }

    private int parseChineseNumber(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        if ("十".equals(text)) {
            return 10;
        }
        if (text.contains("十")) {
            String[] parts = text.split("十", -1);
            int tens = parts[0].isEmpty() ? 1 : chineseDigit(parts[0]);
            int ones = parts.length > 1 && !parts[1].isEmpty() ? chineseDigit(parts[1]) : 0;
            return tens * 10 + ones;
        }
        return chineseDigit(text);
    }

    private int chineseDigit(String text) {
        int value = 0;
        for (char c : text.toCharArray()) {
            switch (c) {
                case '零': value = value * 10; break;
                case '一': value = value * 10 + 1; break;
                case '二':
                case '两': value = value * 10 + 2; break;
                case '三': value = value * 10 + 3; break;
                case '四': value = value * 10 + 4; break;
                case '五': value = value * 10 + 5; break;
                case '六': value = value * 10 + 6; break;
                case '七': value = value * 10 + 7; break;
                case '八': value = value * 10 + 8; break;
                case '九': value = value * 10 + 9; break;
                default: break;
            }
        }
        return value;
    }

    private boolean isReminderRelated(String message) {
        boolean hasExplicitReminderVerb = message.contains("提醒") || message.contains("闹钟") || message.contains("定时") || message.contains("别忘");
        boolean hasTimeExpression = hasRelativeTimeExpression(message) || ABSOLUTE_TIME_PATTERN.matcher(message).find();
        boolean hasSoftReminderVerb = message.contains("记得");

        if (hasExplicitReminderVerb) {
            return true;
        }
        return hasTimeExpression && hasSoftReminderVerb;
    }

    private String extractJSON(String response) {
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
