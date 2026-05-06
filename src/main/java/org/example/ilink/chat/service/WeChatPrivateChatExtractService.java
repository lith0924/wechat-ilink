package org.example.ilink.chat.service;

import java.util.List;

public interface WeChatPrivateChatExtractService {

    /**
     * 列出解密数据库中的私聊消息表
     */
    List<String> listMessageTables(String dbPath);

    /**
     * 预览指定消息表最近若干条原始消息内容
     */
    List<String> previewRawMessages(String dbPath, String tableName, int limit);

    /**
     * 从指定消息表提取私聊 transcript
     */
    String extractTranscript(String dbPath, String tableName, int limit, int incomingOriginSource, int outgoingOriginSource);
}
