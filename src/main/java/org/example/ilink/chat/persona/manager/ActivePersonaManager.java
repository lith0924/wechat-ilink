package org.example.ilink.chat.persona.manager;

import org.example.ilink.chat.persona.domain.PersonaProfile;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class ActivePersonaManager {

    private static final Path STORAGE_PATH = Paths.get("data", "active-persona.txt");

    private volatile PersonaProfile activePersona;
    private volatile String activePersonaName;

    @PostConstruct
    public void loadActivePersonaName() {
        if (!Files.exists(STORAGE_PATH)) {
            return;
        }
        try {
            String value = Files.readString(STORAGE_PATH, StandardCharsets.UTF_8).trim();
            if (!value.isBlank()) {
                activePersonaName = value;
                System.out.println("[ActivePersonaManager] loaded active persona name=" + activePersonaName);
            }
        } catch (IOException e) {
            throw new IllegalStateException("加载 active persona 失败: " + e.getMessage(), e);
        }
    }

    public PersonaProfile getActivePersona() {
        return activePersona;
    }

    public String getActivePersonaName() {
        if (activePersona != null && activePersona.getPersonaName() != null && !activePersona.getPersonaName().isBlank()) {
            return activePersona.getPersonaName();
        }
        return activePersonaName;
    }

    public void setActivePersona(PersonaProfile activePersona) {
        this.activePersona = activePersona;
        this.activePersonaName = activePersona == null ? null : activePersona.getPersonaName();
        persistActivePersonaName();
    }

    public void clear() {
        this.activePersona = null;
        this.activePersonaName = null;
        try {
            Files.deleteIfExists(STORAGE_PATH);
        } catch (IOException e) {
            throw new IllegalStateException("清除 active persona 失败: " + e.getMessage(), e);
        }
    }

    private void persistActivePersonaName() {
        if (activePersonaName == null || activePersonaName.isBlank()) {
            return;
        }
        try {
            Path parent = STORAGE_PATH.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(STORAGE_PATH, activePersonaName, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("持久化 active persona 失败: " + e.getMessage(), e);
        }
    }
}
