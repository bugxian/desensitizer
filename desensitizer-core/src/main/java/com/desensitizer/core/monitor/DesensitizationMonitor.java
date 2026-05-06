package com.desensitizer.core.monitor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class DesensitizationMonitor {

    private final AtomicLong totalCount = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);
    private final Map<String, AtomicLong> typeCounts = new HashMap<>();
    private volatile boolean recordingPaused = false;
    
    private final AtomicLong totalProcessingTime = new AtomicLong(0);
    private final AtomicLong maxProcessingTime = new AtomicLong(0);
    private final AtomicLong minProcessingTime = new AtomicLong(Long.MAX_VALUE);
    
    private final long startTime = System.currentTimeMillis();
    
    // 脱敏案例历史记录（线程安全的双端队列）
    private final Deque<DesensitizationCase> caseHistory = new ConcurrentLinkedDeque<>();
    // 最大存储案例数
    private static final int MAX_HISTORY_SIZE = 10000;
    // 按类型分组的案例统计
    private final Map<String, Deque<DesensitizationCase>> casesByType = new ConcurrentHashMap<>();

    public void recordDesensitization(String type, long processingTimeNs) {
        if (recordingPaused) {
            return;  // 暂停记录时直接返回
        }
        totalCount.incrementAndGet();
        typeCounts.computeIfAbsent(type, k -> new AtomicLong(0)).incrementAndGet();
        totalProcessingTime.addAndGet(processingTimeNs / 1_000_000);
        
        if (processingTimeNs > maxProcessingTime.get()) {
            maxProcessingTime.set(processingTimeNs);
        }
        if (processingTimeNs < minProcessingTime.get() && processingTimeNs > 0) {
            minProcessingTime.set(processingTimeNs);
        }
    }

    public void pauseRecording() {
        this.recordingPaused = true;
    }

    public void resumeRecording() {
        this.recordingPaused = false;
    }

    public boolean isRecordingPaused() {
        return recordingPaused;
    }

    public void recordError() {
        errorCount.incrementAndGet();
    }

    public long getTotalCount() {
        return totalCount.get();
    }

    public long getErrorCount() {
        return errorCount.get();
    }

    public Map<String, Object> getTypeCounts() {
        Map<String, Object> result = new HashMap<>();
        typeCounts.forEach((k, v) -> result.put(k, v.get()));
        return result;
    }

    public long getTotalProcessingTime() {
        return totalProcessingTime.get();
    }

    public long getMaxProcessingTime() {
        return maxProcessingTime.get() / 1_000_000;
    }

    public long getMinProcessingTime() {
        long min = minProcessingTime.get();
        return min == Long.MAX_VALUE ? 0 : min / 1_000_000;
    }

    public long getStartTime() {
        return startTime;
    }

    public void recordDesensitizationCase(String original, String desensitized, String sensitiveType) {
        DesensitizationCase caseRecord = new DesensitizationCase(original, desensitized, sensitiveType);
        
        // 添加到全局历史
        caseHistory.addFirst(caseRecord);
        // 保持最大历史记录数
        while (caseHistory.size() > MAX_HISTORY_SIZE) {
            caseHistory.pollLast();
        }
        
        // 添加到按类型分组的历史
        casesByType.computeIfAbsent(sensitiveType, k -> new ConcurrentLinkedDeque<>()).addFirst(caseRecord);
        // 每个类型最多保留1000条记录
        Deque<DesensitizationCase> typeCases = casesByType.get(sensitiveType);
        while (typeCases.size() > 1000) {
            typeCases.pollLast();
        }
    }

    public List<DesensitizationCase> getRecentCases(int limit) {
        List<DesensitizationCase> result = new ArrayList<>();
        Iterator<DesensitizationCase> iterator = caseHistory.iterator();
        int count = 0;
        while (iterator.hasNext() && count < limit) {
            result.add(iterator.next());
            count++;
        }
        return result;
    }

    public List<DesensitizationCase> getRecentCasesByType(String type, int limit) {
        Deque<DesensitizationCase> typeCases = casesByType.get(type);
        if (typeCases == null) {
            return Collections.emptyList();
        }
        List<DesensitizationCase> result = new ArrayList<>();
        Iterator<DesensitizationCase> iterator = typeCases.iterator();
        int count = 0;
        while (iterator.hasNext() && count < limit) {
            result.add(iterator.next());
            count++;
        }
        return result;
    }

    public Map<String, List<DesensitizationCase>> getAllCasesByType(int limitPerType) {
        Map<String, List<DesensitizationCase>> result = new LinkedHashMap<>();
        for (Map.Entry<String, Deque<DesensitizationCase>> entry : casesByType.entrySet()) {
            List<DesensitizationCase> cases = new ArrayList<>();
            Iterator<DesensitizationCase> iterator = entry.getValue().iterator();
            int count = 0;
            while (iterator.hasNext() && count < limitPerType) {
                cases.add(iterator.next());
                count++;
            }
            result.put(entry.getKey(), cases);
        }
        return result;
    }

    public int getCaseCount() {
        return caseHistory.size();
    }

    public int getCaseCountByType(String type) {
        Deque<DesensitizationCase> typeCases = casesByType.get(type);
        return typeCases != null ? typeCases.size() : 0;
    }

    public void reset() {
        totalCount.set(0);
        errorCount.set(0);
        typeCounts.clear();
        totalProcessingTime.set(0);
        maxProcessingTime.set(0);
        minProcessingTime.set(Long.MAX_VALUE);
        caseHistory.clear();
        casesByType.clear();
    }
}
