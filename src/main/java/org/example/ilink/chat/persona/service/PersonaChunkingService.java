package org.example.ilink.chat.persona.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PersonaChunkingService {

    public List<String> chunkTranscript(String transcript) {
        List<String> lines = new ArrayList<>();
        if (transcript == null || transcript.isBlank()) {
            return lines;
        }

        String[] rawLines = transcript.replace("\r\n", "\n").split("\n");
        for (String rawLine : rawLines) {
            if (rawLine != null && !rawLine.isBlank()) {
                lines.add(rawLine.trim());
            }
        }

        List<String> chunks = new ArrayList<>();
        int windowSize = 4;
        int step = 2;
        for (int start = 0; start < lines.size(); start += step) {
            int end = Math.min(start + windowSize, lines.size());
            if (end - start < 2) {
                continue;
            }
            chunks.add(String.join("\n", lines.subList(start, end)));
            if (end == lines.size()) {
                break;
            }
        }
        return chunks;
    }
}
