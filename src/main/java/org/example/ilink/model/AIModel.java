package org.example.ilink.model;

/**
 * AI 模型统一接口
 * 定义所有 AI 模型必须实现的方法
 */
public interface AIModel {
    
    /**
     * 调用 AI 模型生成回复
     * @param prompt 用户输入的提示词
     * @return AI 模型的回复内容
     */
    String generateResponse(String prompt);
    
    /**
     * 获取模型名称
     * @return 模型名称
     */
    String getModelName();
    
    /**
     * 检查模型是否可用
     * @return true 表示可用，false 表示不可用
     */
    boolean isAvailable();
}
