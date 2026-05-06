package org.example.ilink.chat.persona.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PersonaTranscriptTextParser {

    private static final Pattern MESSAGE_HEADER = Pattern.compile(
            "^\\s*\\d{4}-\\d{2}-\\d{2}\\s+\\d{1,2}:\\d{2}:\\d{2}\\s+['\"]?(.+?)['\"]?\\s*$"
    );

    public String parse(String rawText, String selfSpeaker, String personaSpeaker) {
        if (rawText == null || rawText.isBlank()) {
            throw new IllegalArgumentException("聊天记录文本不能为空");
        }
        String normalizedSelf = normalizeSpeakerName(selfSpeaker, "我");
        String normalizedPersona = normalizeSpeakerName(personaSpeaker, null);
        if (normalizedPersona == null || normalizedPersona.isBlank()) {
            throw new IllegalArgumentException("personaSpeaker 不能为空");
        }

        List<String> transcriptLines = new ArrayList<>();
        String currentSpeaker = null;
        List<String> currentContent = new ArrayList<>();

        String[] lines = rawText.replace("\r\n", "\n").replace('\r', '\n').split("\n");
        for (String line : lines) {
            String trimmed = line == null ? "" : line.trim();
            if (trimmed.isBlank()) {
                flushMessage(transcriptLines, currentSpeaker, currentContent);
                currentSpeaker = null;
                currentContent.clear();
                continue;
            }

            Matcher matcher = MESSAGE_HEADER.matcher(trimmed);
            if (matcher.matches()) {
                flushMessage(transcriptLines, currentSpeaker, currentContent);
                currentSpeaker = normalizeOutputSpeaker(matcher.group(1), normalizedSelf, normalizedPersona);
                currentContent.clear();
                continue;
            }

            if (currentSpeaker != null) {
                currentContent.add(trimmed);
            }
        }
        flushMessage(transcriptLines, currentSpeaker, currentContent);

        if (transcriptLines.isEmpty()) {
            throw new IllegalArgumentException("未能从文本中解析出有效聊天记录，请确认格式为：时间 说话人 换行 消息内容");
        }
        return String.join("\n", transcriptLines);
    }

    private String normalizeOutputSpeaker(String speaker, String selfSpeaker, String personaSpeaker) {
        String normalized = normalizeSpeakerName(speaker, "");
        if (normalized.equals(selfSpeaker) || "我".equals(normalized) || "'我'".equals(normalized) || "\"我\"".equals(normalized)) {
            return "我";
        }
        if (normalized.equals(personaSpeaker)) {
            return "对方";
        }
        return normalized;
    }

    private String normalizeSpeakerName(String speaker, String defaultValue) {
        if (speaker == null || speaker.isBlank()) {
            return defaultValue;
        }
        String normalized = speaker.trim();
        while ((normalized.startsWith("'") && normalized.endsWith("'"))
                || (normalized.startsWith("\"") && normalized.endsWith("\""))) {
            normalized = normalized.substring(1, normalized.length() - 1).trim();
        }
        return normalized;
    }

    private void flushMessage(List<String> transcriptLines, String speaker, List<String> contentLines) {
        if (speaker == null || speaker.isBlank() || contentLines == null || contentLines.isEmpty()) {
            return;
        }
        String content = String.join(" ", contentLines).trim();
        if (!content.isBlank()) {
            transcriptLines.add(speaker + ": " + content);
        }
    }
}
