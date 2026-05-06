package org.example.ilink.chat.persona.domain;

public class PersonaChunk {

    private String id;
    private String personaName;
    private Integer chunkIndex;
    private String content;
    private Double score;

    public PersonaChunk() {
    }

    public PersonaChunk(String id, String personaName, Integer chunkIndex, String content, Double score) {
        this.id = id;
        this.personaName = personaName;
        this.chunkIndex = chunkIndex;
        this.content = content;
        this.score = score;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPersonaName() {
        return personaName;
    }

    public void setPersonaName(String personaName) {
        this.personaName = personaName;
    }

    public Integer getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(Integer chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }
}
