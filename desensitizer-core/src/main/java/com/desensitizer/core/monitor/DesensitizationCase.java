package com.desensitizer.core.monitor;

/**
 * 脱敏案例记录，用于存储每次日志脱敏的前后对比
 */
public class DesensitizationCase {
    
    private final String original;
    private final String desensitized;
    private final String sensitiveType;
    private final long timestamp;
    private final boolean matched;

    public DesensitizationCase(String original, String desensitized, String sensitiveType) {
        this.original = original;
        this.desensitized = desensitized;
        this.sensitiveType = sensitiveType;
        this.timestamp = System.currentTimeMillis();
        this.matched = !original.equals(desensitized);
    }

    public String getOriginal() {
        return original;
    }

    public String getDesensitized() {
        return desensitized;
    }

    public String getSensitiveType() {
        return sensitiveType;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isMatched() {
        return matched;
    }

    @Override
    public String toString() {
        return String.format("DesensitizationCase{type=%s, original='%s', desensitized='%s', matched=%s}", 
                sensitiveType, original, desensitized, matched);
    }
}