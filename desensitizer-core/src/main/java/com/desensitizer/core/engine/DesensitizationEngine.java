package com.desensitizer.core.engine;

import com.desensitizer.core.api.Desensitizer;
import com.desensitizer.core.api.SensitiveDetector;
import com.desensitizer.core.api.SensitiveMatch;
import com.desensitizer.core.api.SensitiveType;
import com.desensitizer.core.monitor.DesensitizationMonitor;
import com.desensitizer.core.registry.DesensitizerRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DesensitizationEngine {

    private final DesensitizerRegistry registry;
    private volatile DesensitizationMonitor monitor;

    public DesensitizationEngine(DesensitizerRegistry registry) {
        this.registry = registry;
    }

    public void setMonitor(DesensitizationMonitor monitor) {
        this.monitor = monitor;
    }

    public DesensitizationMonitor getMonitor() {
        return monitor;
    }

    public String desensitize(String text) {
        return desensitize(text, true);
    }

    public String desensitize(String text, boolean recordToMonitor) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        long startTime = System.nanoTime();
        List<SensitiveMatch> allMatches = new ArrayList<>();

        for (SensitiveDetector detector : registry.getAllDetectors()) {
            if (detector.enabled()) {
                List<SensitiveMatch> matches = detector.detect(text);
                if (matches != null) {
                    allMatches.addAll(matches);
                }
            }
        }

        if (allMatches.isEmpty()) {
            return text;
        }

        Collections.sort(allMatches, Comparator.comparingInt(SensitiveMatch::getStart));

        List<SensitiveMatch> filteredMatches = filterOverlappingMatches(allMatches);

        StringBuilder result = new StringBuilder();
        int lastEnd = 0;
        for (SensitiveMatch match : filteredMatches) {
            int start = match.getStart();
            int end = match.getEnd();

            result.append(text.substring(lastEnd, start));

            String original = text.substring(start, end);
            SensitiveType sensitiveType = SensitiveType.valueOf(match.getSensitiveType());
            Desensitizer desensitizer = registry.getDesensitizer(sensitiveType);
            String masked = desensitizer != null ? desensitizer.desensitize(original) : original;
            result.append(masked);

            lastEnd = end;
        }

        if (lastEnd < text.length()) {
            result.append(text.substring(lastEnd));
        }

        // 记录脱敏案例到监控器（仅在 recordToMonitor 为 true 时）
        if (recordToMonitor && monitor != null && !allMatches.isEmpty()) {
            long processingTime = System.nanoTime() - startTime;
            for (SensitiveMatch match : filteredMatches) {
                String original = text.substring(match.getStart(), match.getEnd());
                SensitiveType sensitiveType = SensitiveType.valueOf(match.getSensitiveType());
                Desensitizer desensitizer = registry.getDesensitizer(sensitiveType);
                String masked = desensitizer != null ? desensitizer.desensitize(original) : original;
                monitor.recordDesensitizationCase(original, masked, sensitiveType.name());
                monitor.recordDesensitization(sensitiveType.name(), processingTime);
            }
        }

        return result.toString();
    }

    private List<SensitiveMatch> filterOverlappingMatches(List<SensitiveMatch> matches) {
        if (matches.isEmpty()) {
            return matches;
        }

        List<SensitiveMatch> filtered = new ArrayList<>();
        int lastStart = -1;
        int lastEnd = -1;

        for (SensitiveMatch match : matches) {
            int start = match.getStart();
            int end = match.getEnd();
            int length = end - start;

            if (start >= lastEnd) {
                filtered.add(match);
                lastStart = start;
                lastEnd = end;
            } else if (length > (lastEnd - lastStart)) {
                filtered.remove(filtered.size() - 1);
                filtered.add(match);
                lastEnd = end;
            }
        }

        return filtered;
    }

    public String desensitize(String text, SensitiveType type) {
        return desensitize(text, type, true);
    }

    public String desensitize(String text, SensitiveType type, boolean recordToMonitor) {
        if (text == null || text.isEmpty() || type == null) {
            return text;
        }
        
        long startTime = System.nanoTime();
        Desensitizer desensitizer = registry.getDesensitizer(type);
        if (desensitizer != null) {
            String result = desensitizer.desensitize(text);
            long processingTime = System.nanoTime() - startTime;
            // 记录脱敏案例到监控器（仅在 recordToMonitor 为 true 时）
            if (recordToMonitor && monitor != null && !text.equals(result)) {
                monitor.recordDesensitizationCase(text, result, type.name());
                monitor.recordDesensitization(type.name(), processingTime);
            }
            return result;
        }
        
        return text;
    }

    public DesensitizerRegistry getRegistry() {
        return registry;
    }
}
