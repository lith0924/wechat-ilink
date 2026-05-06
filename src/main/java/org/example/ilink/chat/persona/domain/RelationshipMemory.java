package org.example.ilink.chat.persona.domain;

import java.util.ArrayList;
import java.util.List;

public class RelationshipMemory {

    private List<String> sharedExperiences = new ArrayList<>();
    private List<String> insideJokes = new ArrayList<>();
    private List<String> nicknames = new ArrayList<>();
    private List<String> recurringTopics = new ArrayList<>();
    private List<String> conflictPatterns = new ArrayList<>();
    private List<String> importantMoments = new ArrayList<>();

    public RelationshipMemory() {
    }

    public List<String> getSharedExperiences() {
        return sharedExperiences;
    }

    public void setSharedExperiences(List<String> sharedExperiences) {
        this.sharedExperiences = sharedExperiences == null ? new ArrayList<>() : sharedExperiences;
    }

    public List<String> getInsideJokes() {
        return insideJokes;
    }

    public void setInsideJokes(List<String> insideJokes) {
        this.insideJokes = insideJokes == null ? new ArrayList<>() : insideJokes;
    }

    public List<String> getNicknames() {
        return nicknames;
    }

    public void setNicknames(List<String> nicknames) {
        this.nicknames = nicknames == null ? new ArrayList<>() : nicknames;
    }

    public List<String> getRecurringTopics() {
        return recurringTopics;
    }

    public void setRecurringTopics(List<String> recurringTopics) {
        this.recurringTopics = recurringTopics == null ? new ArrayList<>() : recurringTopics;
    }

    public List<String> getConflictPatterns() {
        return conflictPatterns;
    }

    public void setConflictPatterns(List<String> conflictPatterns) {
        this.conflictPatterns = conflictPatterns == null ? new ArrayList<>() : conflictPatterns;
    }

    public List<String> getImportantMoments() {
        return importantMoments;
    }

    public void setImportantMoments(List<String> importantMoments) {
        this.importantMoments = importantMoments == null ? new ArrayList<>() : importantMoments;
    }
}
