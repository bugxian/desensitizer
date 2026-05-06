package com.desensitizer.core.registry;

import com.desensitizer.core.api.Desensitizer;
import com.desensitizer.core.api.SensitiveDetector;
import com.desensitizer.core.api.SensitiveMatch;
import com.desensitizer.core.api.SensitiveType;

import java.util.*;

public class DesensitizerRegistry {

    private final Map<SensitiveType, Desensitizer> desensitizers = new EnumMap<>(SensitiveType.class);
    private final Map<SensitiveType, SensitiveDetector> detectors = new EnumMap<>(SensitiveType.class);
    private final List<SensitiveDetector> allDetectors = new ArrayList<>();

    public DesensitizerRegistry() {
    }

    public void register(SensitiveType type, Desensitizer desensitizer) {
        desensitizers.put(type, desensitizer);
    }

    public void registerDetector(SensitiveType type, SensitiveDetector detector) {
        detectors.put(type, detector);
        if (!allDetectors.contains(detector)) {
            allDetectors.add(detector);
        }
    }

    public Desensitizer getDesensitizer(SensitiveType type) {
        return desensitizers.get(type);
    }

    public SensitiveDetector getDetector(SensitiveType type) {
        return detectors.get(type);
    }

    public List<SensitiveDetector> getAllDetectors() {
        return Collections.unmodifiableList(allDetectors);
    }

    public String desensitize(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        List<SensitiveMatch> allMatches = new ArrayList<>();
        for (SensitiveDetector detector : allDetectors) {
            if (detector.enabled()) {
                List<SensitiveMatch> matches = detector.detect(text);
                if (matches != null) {
                    for (SensitiveMatch match : matches) {
                        // 验证匹配位置的有效性
                        if (match.getStart() >= 0 && match.getEnd() <= text.length() && match.getStart() < match.getEnd()) {
                            allMatches.add(match);
                        }
                    }
                }
            }
        }

        if (allMatches.isEmpty()) {
            return text;
        }

        // 按开始位置从小到大排序
        allMatches.sort(Comparator.comparingInt(SensitiveMatch::getStart));

        StringBuilder result = new StringBuilder(text);
        int offset = 0; // 记录替换导致的偏移量

        for (SensitiveMatch match : allMatches) {
            // 计算实际的起始和结束位置（考虑已替换的偏移量）
            int actualStart = match.getStart() + offset;
            int actualEnd = match.getEnd() + offset;

            // 验证位置有效性
            if (actualStart < 0 || actualEnd > result.length() || actualStart >= actualEnd) {
                continue;
            }

            Desensitizer desensitizer = desensitizers.get(SensitiveType.valueOf(match.getSensitiveType()));
            if (desensitizer != null) {
                String originalSubstring = result.substring(actualStart, actualEnd);
                String masked = desensitizer.desensitize(originalSubstring);
                result.replace(actualStart, actualEnd, masked);

                // 更新偏移量
                offset += (masked.length() - (actualEnd - actualStart));
            }
        }
        return result.toString();
    }
}
