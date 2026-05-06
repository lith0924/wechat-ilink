package org.example.ilink.chat.persona.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.ilink.chat.persona.domain.PersonaCore;
import org.example.ilink.chat.persona.domain.PersonaCorrections;
import org.example.ilink.chat.persona.domain.PersonaProfile;
import org.example.ilink.chat.persona.domain.RelationshipMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class PersonaProfileRepository {

    private static final Path STORAGE_PATH = Paths.get("data", "persona-profiles.json");

    private final Map<String, PersonaProfile> store = new ConcurrentHashMap<>();

    @Autowired
    private ObjectMapper objectMapper;

    @PostConstruct
    public void loadFromDisk() {
        if (!Files.exists(STORAGE_PATH)) {
            return;
        }
        try {
            Map<String, PersonaProfile> loaded = objectMapper.readValue(
                    STORAGE_PATH.toFile(),
                    new TypeReference<>() {}
            );
            store.clear();
            if (loaded != null) {
                loaded.forEach((name, profile) -> {
                    if (name != null && profile != null) {
                        normalizeProfile(profile);
                        store.put(name, profile);
                    }
                });
            }
            System.out.println("[PersonaProfileRepository] loaded profiles=" + store.size() + " from " + STORAGE_PATH);
        } catch (IOException e) {
            throw new IllegalStateException("加载 persona profiles 失败: " + e.getMessage(), e);
        }
    }

    public PersonaProfile save(String personaName,
                               String personaCard,
                               String sourceTable,
                               PersonaCore personaCore,
                               RelationshipMemory relationshipMemory,
                               PersonaCorrections corrections) {
        PersonaProfile existing = store.get(personaName);
        PersonaProfile profile = new PersonaProfile(
                personaName,
                personaCard,
                sourceTable,
                personaCore,
                relationshipMemory,
                corrections == null ? new PersonaCorrections() : corrections,
                LocalDateTime.now()
        );
        if (existing != null && existing.getCorrections() != null && corrections == null) {
            profile.setCorrections(existing.getCorrections());
        }
        store.put(personaName, profile);
        persist();
        return profile;
    }

    public PersonaProfile findByName(String personaName) {
        return store.get(personaName);
    }

    public PersonaProfile saveProfile(PersonaProfile profile) {
        if (profile == null || profile.getPersonaName() == null || profile.getPersonaName().isBlank()) {
            throw new IllegalArgumentException("personaProfile 无效");
        }
        profile.setUpdatedAt(LocalDateTime.now());
        if (profile.getCorrections() == null) {
            profile.setCorrections(new PersonaCorrections());
        }
        store.put(profile.getPersonaName(), profile);
        persist();
        return profile;
    }

    private synchronized void persist() {
        try {
            Path parent = STORAGE_PATH.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(STORAGE_PATH.toFile(), store);
        } catch (IOException e) {
            throw new IllegalStateException("持久化 persona profiles 失败: " + e.getMessage(), e);
        }
    }

    private void normalizeProfile(PersonaProfile profile) {
        if (profile.getCorrections() == null) {
            profile.setCorrections(new PersonaCorrections());
        }
        if (profile.getUpdatedAt() == null) {
            profile.setUpdatedAt(LocalDateTime.now());
        }
    }
}
