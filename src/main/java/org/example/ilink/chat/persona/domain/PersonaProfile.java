package org.example.ilink.chat.persona.domain;

import java.time.LocalDateTime;

public class PersonaProfile {

    private String personaName;
    private String personaCard;
    private String sourceTable;
    private PersonaCore personaCore;
    private RelationshipMemory relationshipMemory;
    private PersonaCorrections corrections;
    private LocalDateTime updatedAt;

    public PersonaProfile() {
    }

    public PersonaProfile(String personaName,
                          String personaCard,
                          String sourceTable,
                          PersonaCore personaCore,
                          RelationshipMemory relationshipMemory,
                          PersonaCorrections corrections,
                          LocalDateTime updatedAt) {
        this.personaName = personaName;
        this.personaCard = personaCard;
        this.sourceTable = sourceTable;
        this.personaCore = personaCore;
        this.relationshipMemory = relationshipMemory;
        this.corrections = corrections;
        this.updatedAt = updatedAt;
    }

    public String getPersonaName() {
        return personaName;
    }

    public void setPersonaName(String personaName) {
        this.personaName = personaName;
    }

    public String getPersonaCard() {
        return personaCard;
    }

    public void setPersonaCard(String personaCard) {
        this.personaCard = personaCard;
    }

    public String getSourceTable() {
        return sourceTable;
    }

    public void setSourceTable(String sourceTable) {
        this.sourceTable = sourceTable;
    }

    public PersonaCore getPersonaCore() {
        return personaCore;
    }

    public void setPersonaCore(PersonaCore personaCore) {
        this.personaCore = personaCore;
    }

    public RelationshipMemory getRelationshipMemory() {
        return relationshipMemory;
    }

    public void setRelationshipMemory(RelationshipMemory relationshipMemory) {
        this.relationshipMemory = relationshipMemory;
    }

    public PersonaCorrections getCorrections() {
        return corrections;
    }

    public void setCorrections(PersonaCorrections corrections) {
        this.corrections = corrections;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
