package org.example.ilink.chat.persona.domain;

public class PersonaCore {

    private String identity;
    private String tone;
    private String replyStyle;
    private String emotionalPattern;
    private String carePattern;
    private String tabooPatterns;
    private String signaturePhrases;

    public PersonaCore() {
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public String getTone() {
        return tone;
    }

    public void setTone(String tone) {
        this.tone = tone;
    }

    public String getReplyStyle() {
        return replyStyle;
    }

    public void setReplyStyle(String replyStyle) {
        this.replyStyle = replyStyle;
    }

    public String getEmotionalPattern() {
        return emotionalPattern;
    }

    public void setEmotionalPattern(String emotionalPattern) {
        this.emotionalPattern = emotionalPattern;
    }

    public String getCarePattern() {
        return carePattern;
    }

    public void setCarePattern(String carePattern) {
        this.carePattern = carePattern;
    }

    public String getTabooPatterns() {
        return tabooPatterns;
    }

    public void setTabooPatterns(String tabooPatterns) {
        this.tabooPatterns = tabooPatterns;
    }

    public String getSignaturePhrases() {
        return signaturePhrases;
    }

    public void setSignaturePhrases(String signaturePhrases) {
        this.signaturePhrases = signaturePhrases;
    }
}
