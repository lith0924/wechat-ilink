package org.example.ilink.chat.persona.domain;

import java.util.ArrayList;
import java.util.List;

public class PersonaCorrections {

    private List<String> neverSay = new ArrayList<>();
    private List<String> preferSay = new ArrayList<>();
    private List<String> situationRules = new ArrayList<>();
    private List<String> toneRules = new ArrayList<>();

    public PersonaCorrections() {
    }

    public List<String> getNeverSay() {
        return neverSay;
    }

    public void setNeverSay(List<String> neverSay) {
        this.neverSay = neverSay == null ? new ArrayList<>() : neverSay;
    }

    public List<String> getPreferSay() {
        return preferSay;
    }

    public void setPreferSay(List<String> preferSay) {
        this.preferSay = preferSay == null ? new ArrayList<>() : preferSay;
    }

    public List<String> getSituationRules() {
        return situationRules;
    }

    public void setSituationRules(List<String> situationRules) {
        this.situationRules = situationRules == null ? new ArrayList<>() : situationRules;
    }

    public List<String> getToneRules() {
        return toneRules;
    }

    public void setToneRules(List<String> toneRules) {
        this.toneRules = toneRules == null ? new ArrayList<>() : toneRules;
    }
}
