package org.example.ilink.utils;

/**
 * AI 工具类
 */
public class AIUtils {
    
    /**
     * 检查提示词是否为空
     */
    public static boolean isPromptEmpty(String prompt) {
        return prompt == null || prompt.trim().isEmpty();
    }
    
    /**
     * 清理提示词（去除首尾空格）
     */
    public static String cleanPrompt(String prompt) {
        return prompt == null ? "" : prompt.trim();
    }
    
    /**
     * 截断长提示词
     */
    public static String truncatePrompt(String prompt, int maxLength) {
        if (prompt == null) {
            return "";
        }
        if (prompt.length() > maxLength) {
            return prompt.substring(0, maxLength) + "...";
        }
        return prompt;
    }
    
    /**
     * 验证模型名称
     */
    public static boolean isValidModelName(String modelName) {
        return modelName != null && !modelName.trim().isEmpty();
    }
}
