package com.desensitizer.core.api;

public class SensitiveMatch {
    private int start;
    private int end;
    private String matchedText;
    private String sensitiveType;
    private float confidence;

    public SensitiveMatch() {}

    public SensitiveMatch(int start, int end, String matchedText, String sensitiveType, float confidence) {
        this.start = start;
        this.end = end;
        this.matchedText = matchedText;
        this.sensitiveType = sensitiveType;
        this.confidence = confidence;
    }

    public int getStart() { return start; }
    public void setStart(int start) { this.start = start; }
    public int getEnd() { return end; }
    public void setEnd(int end) { this.end = end; }
    public String getMatchedText() { return matchedText; }
    public void setMatchedText(String matchedText) { this.matchedText = matchedText; }
    public String getSensitiveType() { return sensitiveType; }
    public void setSensitiveType(String sensitiveType) { this.sensitiveType = sensitiveType; }
    public float getConfidence() { return confidence; }
    public void setConfidence(float confidence) { this.confidence = confidence; }
}
