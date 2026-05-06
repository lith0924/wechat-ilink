package org.example.ilink.chat.service.impl;

import org.example.ilink.chat.service.WeChatPrivateChatExtractService;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class WeChatPrivateChatExtractServiceImpl implements WeChatPrivateChatExtractService {

    private static final Pattern MOSTLY_GARBLED_PATTERN = Pattern.compile("^[\\p{Cntrl}\\p{So}\\p{M}\\s\\uFFFD]+$");
    private static final Pattern URL_PATTERN = Pattern.compile("https?://\\S+", Pattern.CASE_INSENSITIVE);
    private static final Pattern GMT_PATTERN = Pattern.compile("\\bGMT[+-]\\d{1,2}:?\\d{0,2}\\b", Pattern.CASE_INSENSITIVE);

    @Override
    public List<String> listMessageTables(String dbPath) {
        String sql = "SELECT name FROM sqlite_master WHERE type='table' AND name LIKE 'Msg_%' ORDER BY name";
        List<String> tables = new ArrayList<>();
        try (Connection connection = open(dbPath);
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                tables.add(rs.getString(1));
            }
        } catch (SQLException e) {
            throw new IllegalArgumentException("读取微信数据库表失败: " + e.getMessage(), e);
        }
        return tables;
    }

    @Override
    public List<String> previewRawMessages(String dbPath, String tableName, int limit) {
        int safeLimit = normalizeLimit(limit);
        String sql = "SELECT origin_source, message_content FROM " + quoteIdentifier(tableName)
                + " ORDER BY local_id DESC LIMIT ?";
        List<String> messages = new ArrayList<>();
        try (Connection connection = open(dbPath);
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, safeLimit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int originSource = rs.getInt("origin_source");
                    String messageContent = rs.getString("message_content");
                    String cleaned = cleanMessageContent(messageContent);
                    if (cleaned == null || cleaned.isBlank()) {
                        continue;
                    }
                    messages.add("origin_source=" + originSource + " | " + cleaned);
                }
            }
        } catch (SQLException e) {
            throw new IllegalArgumentException("预览微信消息失败: " + e.getMessage(), e);
        }
        return messages;
    }

    @Override
    public String extractTranscript(String dbPath, String tableName, int limit, int incomingOriginSource, int outgoingOriginSource) {
        int safeLimit = normalizeLimit(limit);
        String sql = "SELECT local_id, origin_source, message_content FROM " + quoteIdentifier(tableName)
                + " ORDER BY local_id DESC LIMIT ?";

        List<String> lines = new ArrayList<>();
        try (Connection connection = open(dbPath);
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, safeLimit);
            try (ResultSet rs = ps.executeQuery()) {
                List<Row> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(new Row(
                            rs.getLong("local_id"),
                            rs.getInt("origin_source"),
                            rs.getString("message_content")
                    ));
                }
                for (int i = rows.size() - 1; i >= 0; i--) {
                    Row row = rows.get(i);
                    String content = cleanMessageContent(row.messageContent());
                    if (content == null || content.isBlank()) {
                        continue;
                    }
                    String speaker = resolveSpeaker(row.originSource(), incomingOriginSource, outgoingOriginSource);
                    lines.add(speaker + ": " + content);
                }
            }
        } catch (SQLException e) {
            throw new IllegalArgumentException("提取微信私聊 transcript 失败: " + e.getMessage(), e);
        }
        return String.join("\n", lines);
    }

    private Connection open(String dbPath) throws SQLException {
        if (dbPath == null || dbPath.isBlank()) {
            throw new IllegalArgumentException("dbPath 不能为空");
        }
        return DriverManager.getConnection("jdbc:sqlite:" + dbPath);
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return 50;
        }
        return Math.min(limit, 500);
    }

    private String quoteIdentifier(String tableName) {
        if (tableName == null || tableName.isBlank()) {
            throw new IllegalArgumentException("tableName 不能为空");
        }
        if (!tableName.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("非法表名: " + tableName);
        }
        return '"' + tableName + '"';
    }

    private String resolveSpeaker(int originSource, int incomingOriginSource, int outgoingOriginSource) {
        if (originSource == incomingOriginSource) {
            return "对方";
        }
        if (originSource == outgoingOriginSource) {
            return "我";
        }
        return "unknown(" + originSource + ")";
    }

    private String cleanMessageContent(String messageContent) {
        if (messageContent == null) {
            return null;
        }
        String normalized = messageContent.replace("\u0000", "")
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .trim();
        if (normalized.isBlank()) {
            return null;
        }

        String[] lines = normalized.split("\\n+");
        List<String> cleanedLines = new ArrayList<>();
        for (String line : lines) {
            String trimmed = sanitizeLine(line);
            if (trimmed == null || trimmed.isBlank()) {
                continue;
            }
            cleanedLines.add(trimmed);
        }
        return cleanedLines.isEmpty() ? null : String.join(" ", cleanedLines);
    }

    private String sanitizeLine(String line) {
        String trimmed = line == null ? null : line.trim();
        if (trimmed == null || trimmed.isBlank()) {
            return null;
        }
        if ("null".equalsIgnoreCase(trimmed)) {
            return null;
        }
        if (isMostlyGarbled(trimmed)) {
            return null;
        }

        trimmed = trimmed.replaceFirst("^wxid_[A-Za-z0-9]+:", "").trim();
        trimmed = URL_PATTERN.matcher(trimmed).replaceAll(" ").trim();
        trimmed = GMT_PATTERN.matcher(trimmed).replaceAll(" ").trim();
        trimmed = trimmed.replaceAll("\\b\\d{3}-\\d{3,4}-\\d{3,4}\\b", " ").trim();
        trimmed = trimmed.replaceAll("复制该信息.*$", "").trim();
        trimmed = trimmed.replaceAll("点击链接.*$", "").trim();
        trimmed = trimmed.replaceAll("[\\p{Cntrl}]", " ").trim();
        trimmed = trimmed.replaceAll("\\s+", " ").trim();

        if (trimmed.isBlank() || trimmed.length() < 2 || isMostlyGarbled(trimmed)) {
            return null;
        }
        return trimmed;
    }

    private boolean isMostlyGarbled(String value) {
        if (value.length() <= 2) {
            return false;
        }
        if (MOSTLY_GARBLED_PATTERN.matcher(value).matches()) {
            return true;
        }
        int weirdCount = 0;
        for (char c : value.toCharArray()) {
            if (Character.isISOControl(c)) {
                weirdCount++;
                continue;
            }
            if (!Character.isLetterOrDigit(c)
                    && !Character.isWhitespace(c)
                    && "，。！？：；、“”‘’（）()[]【】+-*/_=~@#%&.,!?;:'\"<>|".indexOf(c) < 0
                    && (c < 0x4E00 || c > 0x9FFF)) {
                weirdCount++;
            }
        }
        return weirdCount > value.length() / 2;
    }

    private record Row(long localId, int originSource, String messageContent) {
    }
}
