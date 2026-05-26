package com.desensitizer.spring.controller;

import com.desensitizer.core.engine.DesensitizationEngine;
import com.desensitizer.core.monitor.DesensitizationMonitor;
import com.desensitizer.core.api.SensitiveType;
import com.desensitizer.core.registry.DesensitizerRegistry;
import com.desensitizer.spring.boot.util.ExcelDataLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/desensitizer")
public class DesensitizerConsoleController {

    private static final Logger logger = LoggerFactory.getLogger(DesensitizerConsoleController.class);

    @Autowired(required = false)
    private DesensitizationEngine engine;

    @Autowired(required = false)
    private DesensitizationMonitor monitor;

    @Autowired(required = false)
    private ExcelDataLoader excelDataLoader;

    private volatile TestResultSet testResultSet = null;
    private volatile boolean testRunning = false;

    private static class FieldTestResult {
        final String type;
        final String input;
        final String expected;
        final String actual;
        final boolean passed;
        final int row;

        FieldTestResult(String type, String input, String expected, String actual, int row) {
            this.type = type;
            this.input = input;
            this.expected = expected;
            this.actual = actual;
            this.passed = expected.equals(actual);
            this.row = row;
        }
    }

    private static class TestResultSet {
        final List<FieldTestResult> fieldResults;
        final int totalFields;
        final int passedFields;
        final int failedFields;
        final int totalRows;
        final int desensitizedRows;
        final long testTime;
        final String dataSource;

        TestResultSet(List<FieldTestResult> fieldResults, int totalRows, int desensitizedRows, String dataSource) {
            this.fieldResults = fieldResults;
            this.totalFields = fieldResults.size();
            int passed = 0, failed = 0;
            for (FieldTestResult r : fieldResults) {
                if (r.passed) passed++;
                else failed++;
            }
            this.passedFields = passed;
            this.failedFields = failed;
            this.totalRows = totalRows;
            this.desensitizedRows = desensitizedRows;
            this.testTime = System.currentTimeMillis();
            this.dataSource = dataSource;
        }

        double getAccuracyRate() {
            return totalFields > 0 ? (passedFields * 100.0) / totalFields : 0;
        }

        List<FieldTestResult> getFailedResults() {
            List<FieldTestResult> failed = new ArrayList<>();
            for (FieldTestResult r : fieldResults) {
                if (!r.passed) failed.add(r);
            }
            return failed;
        }

        List<FieldTestResult> getResultsByType(String type) {
            List<FieldTestResult> results = new ArrayList<>();
            for (FieldTestResult r : fieldResults) {
                if (r.type.equals(type)) results.add(r);
            }
            return results;
        }

        Map<String, Integer> getTypeStats() {
            Map<String, Integer> stats = new LinkedHashMap<>();
            for (FieldTestResult r : fieldResults) {
                stats.merge(r.type, 1, Integer::sum);
            }
            return stats;
        }

        Map<String, Integer> getTypePassedStats() {
            Map<String, Integer> stats = new LinkedHashMap<>();
            for (FieldTestResult r : fieldResults) {
                if (r.passed) {
                    stats.merge(r.type, 1, Integer::sum);
                }
            }
            return stats;
        }
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("engineReady", engine != null);
        result.put("monitorReady", monitor != null);
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }

    @GetMapping("/report")
    public Map<String, Object> getReport() {
        logger.info("生成脱敏报告 - 请求时间: {}", new Date());

        Map<String, Object> report = new HashMap<>();
        report.put("reportTime", System.currentTimeMillis());
        report.put("generatedBy", "Desensitizer Console v1.0");

        report.put("accuracy", getAccuracyMetrics());
        report.put("coverage", getCoverageMetrics());
        report.put("performance", getPerformanceMetrics());
        report.put("rules", getRulesMetrics());
        report.put("logCases", getLogDesensitizationCases());
        report.put("summary", getSummary());

        return report;
    }

    @GetMapping("/cases/log")
    public Map<String, Object> getLogCases(Integer limit, String type) {
        Map<String, Object> result = new HashMap<>();
        
        int count = limit != null ? limit : 50;
        
        if (monitor == null) {
            result.put("error", "Monitor not available");
            return result;
        }
        
        List<Map<String, Object>> cases;
        if (type != null && !type.isEmpty()) {
            cases = monitor.getRecentCasesByType(type.toUpperCase(), count).stream()
                .map(this::createCaseMap)
                .collect(Collectors.toList());
        } else {
            cases = monitor.getRecentCases(count).stream()
                .map(this::createCaseMap)
                .collect(Collectors.toList());
        }
        
        result.put("content", cases);
        result.put("total", monitor.getCaseCount());
        result.put("filterType", type);
        
        return result;
    }
    
    private Map<String, Object> getLogDesensitizationCases() {
        Map<String, Object> logCases = new HashMap<>();
        
        if (monitor == null) {
            logCases.put("error", "Monitor not available");
            return logCases;
        }
        
        // 获取按类型分组的日志脱敏案例（最近20条）
        Map<String, List<Map<String, Object>>> casesByType = new LinkedHashMap<>();
        for (Map.Entry<String, List<com.desensitizer.core.monitor.DesensitizationCase>> entry : 
             monitor.getAllCasesByType(20).entrySet()) {
            List<Map<String, Object>> caseList = entry.getValue().stream()
                .map(this::createCaseMap)
                .collect(Collectors.toList());
            casesByType.put(entry.getKey(), caseList);
        }
        
        logCases.put("casesByType", casesByType);
        logCases.put("totalCaseCount", monitor.getCaseCount());
        logCases.put("source", "Logback Appender (Real-time)");
        logCases.put("lastUpdate", System.currentTimeMillis());
        
        return logCases;
    }
    
    private Map<String, Object> createCaseMap(com.desensitizer.core.monitor.DesensitizationCase caseRecord) {
        Map<String, Object> map = new HashMap<>();
        map.put("original", caseRecord.getOriginal());
        map.put("desensitized", caseRecord.getDesensitized());
        map.put("type", caseRecord.getSensitiveType());
        map.put("matched", caseRecord.isMatched());
        map.put("timestamp", caseRecord.getTimestamp());
        return map;
    }

    @GetMapping("/report/html")
    public String getReportHtml() {
        Map<String, Object> report = getReport();
        return generateHtmlReport(report);
    }

    @GetMapping("/metrics")
    public Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        if (monitor != null) {
            metrics.put("totalDesensitized", monitor.getTotalCount());
            metrics.put("totalErrors", monitor.getErrorCount());
            metrics.put("typeDistribution", monitor.getTypeCounts());
            metrics.put("startTime", monitor.getStartTime());
            metrics.put("uptime", System.currentTimeMillis() - monitor.getStartTime());
        } else {
            metrics.put("error", "Monitor not initialized");
        }
        return metrics;
    }

    @GetMapping("/rules")
    public Map<String, Object> getRules() {
        Map<String, Object> rules = new HashMap<>();
        
        List<Map<String, Object>> ruleList = new ArrayList<>();
        for (SensitiveType type : SensitiveType.values()) {
            Map<String, Object> rule = new HashMap<>();
            rule.put("type", type.name());
            rule.put("description", getTypeDescription(type));
            rule.put("enabled", isTypeEnabled(type));
            ruleList.add(rule);
        }
        rules.put("rules", ruleList);
        rules.put("totalRules", ruleList.size());
        
        return rules;
    }

    @GetMapping("/test")
    public Map<String, Object> testDesensitization(String text, String type) {
        Map<String, Object> result = new HashMap<>();
        if (text == null || text.isEmpty()) {
            result.put("error", "text parameter is required");
            return result;
        }

        try {
            String original = text;
            String desensitized;
            
            if (type != null && !type.isEmpty()) {
                SensitiveType sensitiveType = SensitiveType.valueOf(type.toUpperCase());
                desensitized = engine.desensitize(text, sensitiveType);
                result.put("specifiedType", type);
            } else {
                desensitized = engine.desensitize(text);
                result.put("specifiedType", "AUTO");
            }

            result.put("original", original);
            result.put("desensitized", desensitized);
            result.put("changed", !original.equals(desensitized));
            result.put("maskedChars", countMaskedChars(original, desensitized));
            
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    @GetMapping("/test/log")
    public Map<String, Object> testLogDesensitization(String text) {
        Map<String, Object> result = new HashMap<>();
        if (text == null || text.isEmpty()) {
            result.put("error", "text parameter is required");
            return result;
        }

        try {
            long beforeCount = monitor != null ? monitor.getTotalCount() : 0;
            long beforeErrors = monitor != null ? monitor.getErrorCount() : 0;

            // 直接拼接字符串，不使用占位符，确保整个日志消息都被脱敏
            logger.info("脱敏测试: " + text);

            Thread.sleep(100);

            long afterCount = monitor != null ? monitor.getTotalCount() : 0;
            long afterErrors = monitor != null ? monitor.getErrorCount() : 0;

            result.put("original", text);
            result.put("logOutput", "已通过 Logger 输出，请查看控制台日志");
            result.put("monitorCountIncreased", afterCount > beforeCount);
            result.put("monitorErrorIncreased", afterErrors > beforeErrors);
            result.put("beforeCount", beforeCount);
            result.put("afterCount", afterCount);
            result.put("beforeErrors", beforeErrors);
            result.put("afterErrors", afterErrors);
            result.put("monitorUpdated", monitor != null);

        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    @GetMapping("/test/batch")
    public Map<String, Object> testBatchLogDesensitization() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            long beforeCount = monitor != null ? monitor.getTotalCount() : 0;

            logger.info("用户登录: userId=12345, phone=13800138000, name=张三");
            logger.info("订单创建: orderId=202401010001, amount=99.99, bankCard=6222021234567890123");
            logger.info("发送邮件: to=user@example.com, subject=订单确认");
            logger.info("用户注册: idCard=110101199001011234, address=北京市朝阳区建国路88号");

            Thread.sleep(200);

            long afterCount = monitor != null ? monitor.getTotalCount() : 0;

            result.put("testScenarios", Arrays.asList(
                "用户登录日志",
                "订单创建日志", 
                "邮件发送日志",
                "用户注册日志"
            ));
            result.put("beforeCount", beforeCount);
            result.put("afterCount", afterCount);
            result.put("expectedIncrease", 4);
            result.put("actualIncrease", afterCount - beforeCount);
            result.put("success", (afterCount - beforeCount) >= 4);

        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    @GetMapping("/test/excel")
    public Map<String, Object> testExcelDataDesensitization() {
        Map<String, Object> result = new HashMap<>();
        
        if (excelDataLoader == null) {
            result.put("error", "ExcelDataLoader not available");
            return result;
        }

        if (testRunning) {
            result.put("error", "测试正在执行中，请稍后");
            return result;
        }

        try {
            testRunning = true;
            long beforeCount = monitor != null ? monitor.getTotalCount() : 0;

            List<ExcelDataLoader.LogEntry> logEntries = excelDataLoader.loadFromExcel();
            
            if (logEntries.isEmpty()) {
                result.put("error", "未能从Excel文件加载测试数据");
                return result;
            }

            List<FieldTestResult> fieldResults = new ArrayList<>();
            List<Map<String, Object>> rowDetails = new ArrayList<>();
            int desensitizedCount = 0;

            for (int i = 0; i < logEntries.size(); i++) {
                ExcelDataLoader.LogEntry entry = logEntries.get(i);
                String logString = entry.toLogString();
                
                long entryBefore = monitor != null ? monitor.getTotalCount() : 0;
                logger.info("Excel测试数据[" + (i + 1) + "]: " + logString);
                Thread.sleep(10);
                long entryAfter = monitor != null ? monitor.getTotalCount() : 0;

                boolean rowDesensitized = entryAfter > entryBefore;
                if (rowDesensitized) {
                    desensitizedCount++;
                }

                String desensitizedLog = engine.desensitize(logString, false);

                if (entry.getName() != null && !entry.getName().isEmpty() && entry.getNameDesensitized() != null) {
                    String actualName = engine.desensitize(entry.getName(), SensitiveType.NAME, false);
                    fieldResults.add(new FieldTestResult("NAME", entry.getName(), entry.getNameDesensitized(), actualName, i + 1));
                }
                if (entry.getPhone() != null && !entry.getPhone().isEmpty() && entry.getPhoneDesensitized() != null) {
                    String actualPhone = engine.desensitize(entry.getPhone(), SensitiveType.PHONE, false);
                    fieldResults.add(new FieldTestResult("PHONE", entry.getPhone(), entry.getPhoneDesensitized(), actualPhone, i + 1));
                }
                if (entry.getIdCard() != null && !entry.getIdCard().isEmpty() && entry.getIdCardDesensitized() != null) {
                    String actualIdCard = engine.desensitize(entry.getIdCard(), SensitiveType.ID_CARD, false);
                    fieldResults.add(new FieldTestResult("ID_CARD", entry.getIdCard(), entry.getIdCardDesensitized(), actualIdCard, i + 1));
                }
                if (entry.getBankCard() != null && !entry.getBankCard().isEmpty() && entry.getBankCardDesensitized() != null) {
                    String actualBankCard = engine.desensitize(entry.getBankCard(), SensitiveType.BANK_CARD, false);
                    fieldResults.add(new FieldTestResult("BANK_CARD", entry.getBankCard(), entry.getBankCardDesensitized(), actualBankCard, i + 1));
                }
                if (entry.getAddress() != null && !entry.getAddress().isEmpty() && entry.getAddressDesensitized() != null) {
                    String actualAddress = engine.desensitize(entry.getAddress(), SensitiveType.ADDRESS, false);
                    fieldResults.add(new FieldTestResult("ADDRESS", entry.getAddress(), entry.getAddressDesensitized(), actualAddress, i + 1));
                }

                Map<String, Object> entryResult = new HashMap<>();
                entryResult.put("row", i + 1);
                entryResult.put("original", logString);
                entryResult.put("desensitized", rowDesensitized);
                rowDetails.add(entryResult);
            }

            Thread.sleep(100);
            long afterCount = monitor != null ? monitor.getTotalCount() : 0;

            String dataSource = "test-data/赛题4-支持敏感信息脱敏的通用工具-测试数据v0.3.xlsx";
            testResultSet = new TestResultSet(fieldResults, logEntries.size(), desensitizedCount, dataSource);

            result.put("excelFile", dataSource);
            result.put("totalRows", logEntries.size());
            result.put("desensitizedRows", desensitizedCount);
            result.put("totalFields", testResultSet.totalFields);
            result.put("passedFields", testResultSet.passedFields);
            result.put("failedFields", testResultSet.failedFields);
            result.put("accuracyRate", String.format("%.2f%%", testResultSet.getAccuracyRate()));
            result.put("beforeCount", beforeCount);
            result.put("afterCount", afterCount);
            result.put("totalIncrease", afterCount - beforeCount);
            result.put("success", desensitizedCount == logEntries.size());
            result.put("details", rowDetails);

        } catch (Exception e) {
            result.put("error", e.getMessage());
            e.printStackTrace();
        } finally {
            testRunning = false;
        }
        
        return result;
    }

    @GetMapping("/test/cases")
    public Map<String, Object> getTestCases(Integer page, Integer size, String type) {
        Map<String, Object> result = new HashMap<>();
        
        int pageNum = page != null ? page : 1;
        int pageSize = size != null ? size : 20;

        if (testResultSet == null) {
            result.put("content", Collections.emptyList());
            result.put("page", pageNum);
            result.put("size", pageSize);
            result.put("totalElements", 0);
            result.put("totalPages", 0);
            result.put("hasNext", false);
            result.put("hasPrevious", false);
            result.put("testExecuted", false);
            return result;
        }

        List<FieldTestResult> filtered = testResultSet.fieldResults;
        if (type != null && !type.isEmpty()) {
            filtered = testResultSet.getResultsByType(type.toUpperCase());
        }
        
        int totalElements = filtered.size();
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);
        int start = (pageNum - 1) * pageSize;
        int end = Math.min(start + pageSize, totalElements);
        
        List<Map<String, String>> pageTestCases = new ArrayList<>();
        for (int i = start; i < end; i++) {
            FieldTestResult fr = filtered.get(i);
            pageTestCases.add(createTestCase(fr.type, fr.input, fr.expected, fr.actual, fr.passed));
        }
        
        result.put("content", pageTestCases);
        result.put("page", pageNum);
        result.put("size", pageSize);
        result.put("totalElements", totalElements);
        result.put("totalPages", totalPages);
        result.put("hasNext", pageNum < totalPages);
        result.put("hasPrevious", pageNum > 1);
        result.put("filterType", type);
        result.put("testExecuted", true);
        
        return result;
    }

    @GetMapping("/test/failed-cases")
    public Map<String, Object> getFailedCases(Integer page, Integer size) {
        Map<String, Object> result = new HashMap<>();
        
        int pageNum = page != null ? page : 1;
        int pageSize = size != null ? size : 20;

        if (testResultSet == null) {
            result.put("content", Collections.emptyList());
            result.put("page", pageNum);
            result.put("size", pageSize);
            result.put("totalElements", 0);
            result.put("totalPages", 0);
            result.put("hasNext", false);
            result.put("hasPrevious", false);
            return result;
        }

        List<FieldTestResult> allFailed = testResultSet.getFailedResults();
        
        int totalElements = allFailed.size();
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);
        int start = (pageNum - 1) * pageSize;
        int end = Math.min(start + pageSize, totalElements);
        
        List<Map<String, Object>> pageFailedCases = new ArrayList<>();
        for (int i = start; i < end; i++) {
            FieldTestResult fr = allFailed.get(i);
            Map<String, Object> failedCase = new HashMap<>();
            failedCase.put("rule", fr.type);
            failedCase.put("input", fr.input);
            failedCase.put("expected", fr.expected);
            failedCase.put("actual", fr.actual);
            pageFailedCases.add(failedCase);
        }
        
        result.put("content", pageFailedCases);
        result.put("page", pageNum);
        result.put("size", pageSize);
        result.put("totalElements", totalElements);
        result.put("totalPages", totalPages);
        result.put("hasNext", pageNum < totalPages);
        result.put("hasPrevious", pageNum > 1);
        
        return result;
    }

    private Map<String, Object> getAccuracyMetrics() {
        Map<String, Object> accuracy = new HashMap<>();

        if (testResultSet == null) {
            accuracy.put("accuracyRate", "N/A");
            accuracy.put("totalValidationTests", 0);
            accuracy.put("passedTests", 0);
            accuracy.put("failedTests", 0);
            accuracy.put("lastValidationTime", 0L);
            accuracy.put("sampleTestCases", Collections.emptyList());
            accuracy.put("testDataSource", "未执行测试");
            accuracy.put("totalPages", 0);
            accuracy.put("testExecuted", false);
            return accuracy;
        }

        List<Map<String, String>> sampleCases = new ArrayList<>();
        int displayCount = Math.min(20, testResultSet.fieldResults.size());
        for (int i = 0; i < displayCount; i++) {
            FieldTestResult fr = testResultSet.fieldResults.get(i);
            sampleCases.add(createTestCase(fr.type, fr.input, fr.expected, fr.actual, fr.passed));
        }
        
        accuracy.put("accuracyRate", String.format("%.2f%%", testResultSet.getAccuracyRate()));
        accuracy.put("totalValidationTests", testResultSet.totalFields);
        accuracy.put("passedTests", testResultSet.passedFields);
        accuracy.put("failedTests", testResultSet.failedFields);
        accuracy.put("lastValidationTime", testResultSet.testTime);
        accuracy.put("sampleTestCases", sampleCases);
        accuracy.put("testDataSource", testResultSet.dataSource);
        accuracy.put("totalPages", (int) Math.ceil((double) testResultSet.totalFields / 20));
        accuracy.put("testExecuted", true);
        
        return accuracy;
    }

    private Map<String, Object> getCoverageMetrics() {
        Map<String, Object> coverage = new HashMap<>();

        if (testResultSet == null) {
            coverage.put("ruleCoverage", Collections.emptyList());
            coverage.put("overallCoverageRate", "N/A");
            coverage.put("coveredPatterns", 0);
            coverage.put("totalPatterns", 6);
            coverage.put("validSamplesCount", 0);
            coverage.put("invalidSamplesCount", 0);
            coverage.put("testExecuted", false);
            return coverage;
        }

        List<Map<String, Object>> ruleCoverage = new ArrayList<>();
        String[] ruleOrder = {"PHONE", "ID_CARD", "BANK_CARD", "EMAIL", "NAME", "ADDRESS"};
        for (String rule : ruleOrder) {
            ruleCoverage.add(calculateCoverageFromResults(rule));
        }

        int totalCoverage = 0;
        int coveredPatterns = 0;
        int totalSamples = 0;

        for (Map<String, Object> rc : ruleCoverage) {
            String rateStr = (String) rc.get("coverageRate");
            int rate = Integer.parseInt(rateStr.replace("%", ""));
            totalCoverage += rate;
            if (rate > 0) coveredPatterns++;
            totalSamples += (Integer) rc.get("validCount");
        }

        coverage.put("ruleCoverage", ruleCoverage);
        coverage.put("overallCoverageRate", String.format("%.2f%%", coveredPatterns > 0 ? (totalCoverage * 1.0) / coveredPatterns : 0));
        coverage.put("coveredPatterns", coveredPatterns);
        coverage.put("totalPatterns", 6);
        coverage.put("validSamplesCount", totalSamples);
        coverage.put("invalidSamplesCount", 0);
        coverage.put("testExecuted", true);

        return coverage;
    }

    private Map<String, Object> calculateCoverageFromResults(String rule) {
        Map<String, Object> item = new HashMap<>();
        item.put("rule", rule);

        List<FieldTestResult> typeResults = testResultSet.getResultsByType(rule);
        int totalCases = typeResults.size();
        int passedCases = 0;
        List<Map<String, String>> failedCases = new ArrayList<>();
        for (FieldTestResult fr : typeResults) {
            if (fr.passed) {
                passedCases++;
            } else {
                Map<String, String> failedCase = new HashMap<>();
                failedCase.put("input", fr.input);
                failedCase.put("expected", fr.expected);
                failedCase.put("actual", fr.actual);
                failedCases.add(failedCase);
            }
        }
        item.put("failedCases", failedCases);

        int coverage = totalCases > 0 ? (passedCases * 100) / totalCases : 0;
        int casePassRate = coverage;

        item.put("coverageRate", coverage + "%");
        item.put("testCasePassRate", String.format("%d/%d (%d%%)", passedCases, totalCases, casePassRate));
        item.put("description", String.format("测试用例: %d/%d (%.1f%%) | 未通过: %d",
            passedCases, totalCases, totalCases > 0 ? (passedCases * 100.0 / totalCases) : 0,
            totalCases - passedCases));
        item.put("validCount", totalCases);
        item.put("detectedCount", passedCases);
        item.put("totalCases", totalCases);
        item.put("passedCases", passedCases);

        String status;
        if (totalCases == 0) {
            status = "DISABLED";
        } else if (casePassRate >= 90) {
            status = "EXCELLENT";
        } else if (casePassRate >= 70) {
            status = "GOOD";
        } else if (casePassRate > 0) {
            status = "NEEDS_IMPROVEMENT";
        } else {
            status = "DISABLED";
        }
        item.put("status", status);

        return item;
    }

    private Map<String, Object> getPerformanceMetrics() {
        Map<String, Object> performance = new HashMap<>();
        
        if (monitor != null) {
            long totalTimeNs = monitor.getTotalProcessingTime();
            long totalCount = monitor.getTotalCount();
            // 纳秒转换为毫秒，保留4位小数
            double avgTimeMs = totalCount > 0 ? (totalTimeNs / 1_000_000.0 / totalCount) : 0;
            
            performance.put("totalProcessed", totalCount);
            performance.put("desensitizedLogs", monitor.getDesensitizedLogCount());
            performance.put("totalProcessingTimeMs", totalTimeNs / 1_000_000);
            performance.put("averageTimePerRequestMs", String.format("%.4f", avgTimeMs));
            performance.put("maxProcessingTimeMs", monitor.getMaxProcessingTime());
            performance.put("minProcessingTimeMs", monitor.getMinProcessingTime());
            double throughput = monitor.getSlidingWindowThroughput();
            double overallThroughput = monitor.getOverallThroughput();
            performance.put("throughputPerSecond", throughput > 0 ? String.format("%.2f", throughput) : "0.00");
            performance.put("overallThroughputPerSecond", String.format("%.2f", overallThroughput));
        } else {
            performance.put("totalProcessed", 0);
            performance.put("totalProcessingTimeMs", 0);
            performance.put("averageTimePerRequestMs", "N/A");
            performance.put("maxProcessingTimeMs", 0);
            performance.put("minProcessingTimeMs", 0);
            performance.put("throughputPerSecond", "0.00");
            performance.put("overallThroughputPerSecond", "0.00");
            performance.put("note", "Performance metrics will be available after first desensitization");
        }
        
        performance.put("performanceLevel", getPerformanceLevel(performance));
        performance.put("recommendations", getPerformanceRecommendations(performance));
        
        return performance;
    }

    private Map<String, Object> getRulesMetrics() {
        Map<String, Object> rules = new HashMap<>();
        
        Map<String, Object> typeCounts = monitor != null ? monitor.getTypeCounts() : new HashMap<>();
        
        List<Map<String, Object>> ruleStats = new ArrayList<>();
        for (SensitiveType type : SensitiveType.values()) {
            if (type == SensitiveType.CUSTOM) continue;
            
            long count = typeCounts.containsKey(type.name()) ? 
                (Long) typeCounts.get(type.name()) : 0;
            ruleStats.add(createRuleStat(type.name(), getTypeDescription(type), count));
        }
        
        rules.put("ruleStatistics", ruleStats);
        
        if (monitor != null) {
            rules.put("totalDesensitized", monitor.getTotalCount());
            rules.put("errorCount", monitor.getErrorCount());
            rules.put("errorRate", monitor.getTotalCount() > 0 ? 
                String.format("%.2f%%", (monitor.getErrorCount() * 100.0) / monitor.getTotalCount()) : "0%");
        } else {
            rules.put("totalDesensitized", 0);
            rules.put("errorCount", 0);
            rules.put("errorRate", "0%");
        }
        
        return rules;
    }

    private Map<String, Object> getSummary() {
        Map<String, Object> summary = new HashMap<>();
        
        Map<String, Object> accuracy = getAccuracyMetrics();
        Map<String, Object> performance = getPerformanceMetrics();
        Map<String, Object> rules = getRulesMetrics();
        
        double accuracyRate;
        String accuracyRateStr = (String) accuracy.get("accuracyRate");
        if ("N/A".equals(accuracyRateStr)) {
            accuracyRate = 0;
        } else {
            accuracyRate = Double.parseDouble(accuracyRateStr.replace("%", ""));
        }
        long totalDesensitized = (Long) rules.get("totalDesensitized");
        long errorCount = (Long) rules.get("errorCount");
        
        String grade;
        String overallStatus;
        String confidence;
        String message;

        boolean testExecuted = Boolean.TRUE.equals(accuracy.get("testExecuted"));

        if (!testExecuted) {
            grade = "-";
            overallStatus = "PENDING";
            confidence = "N/A";
            message = "尚未执行日志脱敏测试，请先调用 /desensitizer/test/excel";
        } else if (accuracyRate >= 95 && errorCount == 0) {
            grade = "A";
            overallStatus = "HEALTHY";
            confidence = "HIGH";
            message = "脱敏系统运行正常，所有规则校验通过";
        } else if (accuracyRate >= 85) {
            grade = "B";
            overallStatus = "NORMAL";
            confidence = "MEDIUM";
            message = "脱敏系统运行正常，建议关注部分规则";
        } else if (accuracyRate >= 70) {
            grade = "C";
            overallStatus = "CAUTION";
            confidence = "LOW";
            message = "部分脱敏规则需要优化，请检查配置";
        } else {
            grade = "D";
            overallStatus = "WARNING";
            confidence = "VERY_LOW";
            message = "脱敏系统存在严重问题，请立即检查";
        }
        
        summary.put("overallStatus", overallStatus);
        summary.put("grade", grade);
        summary.put("confidence", confidence);
        summary.put("message", message);
        summary.put("accuracyRate", accuracy.get("accuracyRate"));
        summary.put("totalDesensitized", totalDesensitized);
        summary.put("errorCount", errorCount);
        
        return summary;
    }

    private boolean isTypeEnabled(SensitiveType type) {
        if (engine == null) return false;
        DesensitizerRegistry registry = engine.getRegistry();
        return registry.getDesensitizer(type) != null;
    }

    private Map<String, String> createTestCase(String type, String input, String expected, String actual, boolean passed) {
        Map<String, String> testCase = new HashMap<>();
        testCase.put("type", type);
        testCase.put("input", input);
        testCase.put("expected", expected);
        testCase.put("actual", actual);
        testCase.put("passed", String.valueOf(passed));
        return testCase;
    }

    private Map<String, Object> createRuleStat(String type, String description, long count) {
        Map<String, Object> stat = new HashMap<>();
        stat.put("type", type);
        stat.put("description", description);
        stat.put("count", count);
        stat.put("percentage", "0%");
        return stat;
    }

    private String getTypeDescription(SensitiveType type) {
        switch (type) {
            case PHONE: return "手机号脱敏";
            case ID_CARD: return "身份证号脱敏";
            case BANK_CARD: return "银行卡号脱敏";
            case EMAIL: return "邮箱地址脱敏";
            case PASSWORD: return "密码脱敏";
            case ADDRESS: return "地址脱敏";
            case NAME: return "姓名脱敏";
            case CUSTOM: return "自定义脱敏";
            default: return "未知类型";
        }
    }

    private int countMaskedChars(String original, String desensitized) {
        int count = 0;
        for (int i = 0; i < Math.min(original.length(), desensitized.length()); i++) {
            if (original.charAt(i) != desensitized.charAt(i) && desensitized.charAt(i) == '*') {
                count++;
            }
        }
        return count;
    }

    private String getPerformanceLevel(Map<String, Object> performance) {
        String avgTimeStr = (String) performance.get("averageTimePerRequestMs");
        if ("N/A".equals(avgTimeStr)) return "UNKNOWN";
        
        double avgTime = Double.parseDouble(avgTimeStr);
        if (avgTime < 1) return "EXCELLENT";
        if (avgTime < 5) return "GOOD";
        if (avgTime < 50) return "ACCEPTABLE";
        return "NEEDS_OPTIMIZATION";
    }

    private List<String> getPerformanceRecommendations(Map<String, Object> performance) {
        List<String> recommendations = new ArrayList<>();
        String level = getPerformanceLevel(performance);
        
        if ("NEEDS_OPTIMIZATION".equals(level)) {
            recommendations.add("考虑启用异步脱敏模式");
            recommendations.add("检查是否有过多的自定义规则");
            recommendations.add("考虑增加缓存策略");
        }
        
        recommendations.add("当前系统性能良好");
        recommendations.add("建议定期监控性能指标");
        
        return recommendations;
    }

    private String generateHtmlReport(Map<String, Object> report) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html lang=\"zh-CN\">\n<head>\n");
        html.append("<meta charset=\"UTF-8\">\n");
        html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("<title>脱敏规则校验报告 - Desensitizer Console</title>\n");
        html.append("<style>\n");
        html.append("* { margin: 0; padding: 0; box-sizing: border-box; }\n");
        html.append("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); min-height: 100vh; padding: 20px; }\n");
        html.append(".container { max-width: 1200px; margin: 0 auto; }\n");
        html.append(".header { background: white; border-radius: 16px; padding: 30px; margin-bottom: 20px; box-shadow: 0 10px 40px rgba(0,0,0,0.1); }\n");
        html.append(".header h1 { color: #333; font-size: 28px; margin-bottom: 10px; }\n");
        html.append(".header p { color: #666; font-size: 14px; }\n");
        html.append(".header .meta { margin-top: 15px; padding-top: 15px; border-top: 1px solid #eee; display: flex; gap: 30px; color: #888; font-size: 13px; flex-wrap: wrap; }\n");
        html.append(".card { background: white; border-radius: 16px; padding: 25px; margin-bottom: 20px; box-shadow: 0 10px 40px rgba(0,0,0,0.1); }\n");
        html.append(".card h2 { color: #333; font-size: 20px; margin-bottom: 20px; display: flex; align-items: center; gap: 10px; }\n");
        html.append(".card h2::before { content: ''; width: 4px; height: 20px; background: linear-gradient(135deg, #667eea, #764ba2); border-radius: 2px; }\n");
        html.append(".metric-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px; }\n");
        html.append(".metric-card { background: linear-gradient(135deg, #f5f7fa 0%, #e4e8ec 100%); border-radius: 12px; padding: 20px; text-align: center; position: relative; cursor: pointer; transition: transform 0.2s, box-shadow 0.2s; }\n");
        html.append(".metric-card:hover { transform: translateY(-3px); box-shadow: 0 8px 25px rgba(102, 126, 234, 0.2); }\n");
        html.append(".metric-card .value { font-size: 32px; font-weight: 700; color: #667eea; margin-bottom: 5px; }\n");
        html.append(".metric-card .label { font-size: 13px; color: #666; }\n");
        html.append(".metric-card .tooltip { display: none; position: absolute; bottom: calc(100% + 10px); left: 50%; transform: translateX(-50%); background: #333; color: white; padding: 10px 15px; border-radius: 8px; font-size: 12px; white-space: nowrap; z-index: 100; }\n");
        html.append(".metric-card .tooltip::after { content: ''; position: absolute; top: 100%; left: 50%; transform: translateX(-50%); border: 6px solid transparent; border-top-color: #333; }\n");
        html.append(".metric-card:hover .tooltip { display: block; }\n");
        html.append(".table { width: 100%; border-collapse: collapse; margin-top: 10px; }\n");
        html.append(".table th, .table td { padding: 12px; text-align: left; border-bottom: 1px solid #eee; }\n");
        html.append(".table th { background: #f8f9fa; font-weight: 600; color: #333; }\n");
        html.append(".table tr:hover { background: #f8f9fa; }\n");
        html.append(".status-badge { display: inline-block; padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: 600; }\n");
        html.append(".status-excellent { background: #d4edda; color: #155724; }\n");
        html.append(".status-good { background: #d1ecf1; color: #0c5460; }\n");
        html.append(".status-acceptable { background: #fff3cd; color: #856404; }\n");
        html.append(".status-warning { background: #f8d7da; color: #721c24; }\n");
        html.append(".summary { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); border-radius: 16px; padding: 30px; color: white; text-align: center; }\n");
        html.append(".summary .grade { font-size: 72px; font-weight: 700; opacity: 0.9; }\n");
        html.append(".summary .status { font-size: 24px; margin-top: 10px; margin-bottom: 10px; }\n");
        html.append(".summary .confidence { font-size: 14px; opacity: 0.8; }\n");
        html.append(".test-input { margin-top: 15px; display: flex; gap: 10px; flex-wrap: wrap; }\n");
        html.append(".test-input input { flex: 1; min-width: 200px; padding: 12px; border: 2px solid #eee; border-radius: 8px; font-size: 14px; transition: border-color 0.3s; }\n");
        html.append(".test-input input:focus { outline: none; border-color: #667eea; }\n");
        html.append(".test-input select { padding: 12px; border: 2px solid #eee; border-radius: 8px; font-size: 14px; }\n");
        html.append(".test-input button { padding: 12px 30px; background: linear-gradient(135deg, #667eea, #764ba2); color: white; border: none; border-radius: 8px; cursor: pointer; font-size: 14px; font-weight: 600; transition: transform 0.2s; }\n");
        html.append(".test-input button:hover { transform: translateY(-2px); }\n");
        html.append(".test-result { margin-top: 15px; padding: 15px; background: #f8f9fa; border-radius: 8px; }\n");
        html.append(".test-result .original { color: #666; margin-bottom: 5px; }\n");
        html.append(".test-result .desensitized { color: #667eea; font-weight: 600; }\n");
        html.append(".badge-success { background: #28a745; color: white; }\n");
        html.append(".badge-failed { background: #dc3545; color: white; }\n");
        html.append(".footer { text-align: center; color: rgba(255,255,255,0.7); font-size: 13px; margin-top: 30px; }\n");
        html.append(".source-info { font-size: 12px; color: #888; margin-top: 10px; }\n");
        html.append(".pagination { display: flex; justify-content: center; align-items: center; gap: 5px; margin-top: 20px; flex-wrap: wrap; }\n");
        html.append(".pagination button { padding: 8px 16px; border: 1px solid #ddd; border-radius: 4px; background: white; cursor: pointer; font-size: 14px; }\n");
        html.append(".pagination button:hover:not(:disabled) { background: #f8f9fa; }\n");
        html.append(".pagination button:disabled { opacity: 0.5; cursor: not-allowed; }\n");
        html.append(".pagination .active { background: #667eea; color: white; border-color: #667eea; }\n");
        html.append(".pagination-info { text-align: center; color: #666; font-size: 13px; margin-top: 10px; }\n");
        html.append(".filter-bar { display: flex; gap: 10px; align-items: center; margin-bottom: 15px; flex-wrap: wrap; }\n");
        html.append(".filter-bar select, .filter-bar input { padding: 8px 12px; border: 1px solid #ddd; border-radius: 4px; font-size: 14px; }\n");
        html.append(".filter-bar button { padding: 8px 20px; background: #667eea; color: white; border: none; border-radius: 4px; cursor: pointer; }\n");
        html.append("</style>\n");
        html.append("</head>\n<body>\n");

        html.append("<div class=\"container\">\n");
        
        html.append("<div class=\"header\">\n");
        html.append("<h1>🔒 脱敏规则校验报告</h1>\n");
        html.append("<p>Desensitizer Console - 实时监控与规则校验</p>\n");
        html.append("<div class=\"meta\">\n");
        html.append("<span>生成时间: ").append(new Date().toString()).append("</span>\n");
        html.append("<span>版本: v1.0.0-SNAPSHOT</span>\n");
        html.append("<span>状态: ✅ 运行正常</span>\n");
        html.append("</div>\n");
        html.append("</div>\n");

        Map<String, Object> summary = (Map<String, Object>) report.get("summary");
        html.append("<div class=\"summary\">\n");
        html.append("<div class=\"grade\">").append(summary.get("grade")).append("</div>\n");
        html.append("<div class=\"status\">").append(summary.get("overallStatus")).append("</div>\n");
        html.append("<div class=\"confidence\">置信度: ").append(summary.get("confidence")).append(" | ").append(summary.get("message")).append("</div>\n");
        html.append("</div>\n");

        Map<String, Object> accuracy = (Map<String, Object>) report.get("accuracy");
        html.append("<div class=\"card\">\n");
        html.append("<h2>🎯 脱敏准确率 <span style=\"font-size: 13px; color: #888; font-weight: normal;\">（基于日志脱敏验证）</span></h2>\n");
        html.append("<div class=\"metric-grid\">\n");
        if (Boolean.TRUE.equals(accuracy.get("testExecuted"))) {
            html.append("<div class=\"metric-card\"><div class=\"value\">").append(accuracy.get("accuracyRate")).append("</div><div class=\"label\">准确率</div></div>\n");
            html.append("<div class=\"metric-card\"><div class=\"value\">").append(accuracy.get("passedTests")).append("/").append(accuracy.get("totalValidationTests")).append("</div><div class=\"label\">字段验证通过</div></div>\n");
            html.append("<div class=\"metric-card\"><div class=\"value\">").append(accuracy.get("failedTests")).append("</div><div class=\"label\">字段验证失败</div></div>\n");
        } else {
            html.append("<div class=\"metric-card\" style=\"grid-column: 1 / -1;\"><div class=\"value\" style=\"font-size: 18px; color: #888;\">尚未执行测试</div><div class=\"label\">请先调用 /desensitizer/test/excel 执行日志脱敏测试</div></div>\n");
        }
        html.append("</div>\n");
        html.append("<div class=\"source-info\">统计说明: 准确率基于日志脱敏验证——通过 logger 输出测试数据触发 Appender 脱敏，再逐字段比对实际脱敏结果与期望值。测试数据源: ").append(accuracy.get("testDataSource")).append("</div>\n");
        
        html.append("<div class=\"filter-bar\">\n");
        html.append("<select id=\"filterType\" onchange=\"loadTestCases(1)\">\n");
        html.append("<option value=\"\">全部类型</option>\n");
        html.append("<option value=\"PHONE\">PHONE</option>\n");
        html.append("<option value=\"ID_CARD\">ID_CARD</option>\n");
        html.append("<option value=\"BANK_CARD\">BANK_CARD</option>\n");
        html.append("<option value=\"NAME\">NAME</option>\n");
        html.append("<option value=\"ADDRESS\">ADDRESS</option>\n");
        html.append("</select>\n");
        html.append("<select id=\"pageSize\" onchange=\"loadTestCases(1)\">\n");
        html.append("<option value=\"10\">每页10条</option>\n");
        html.append("<option value=\"20\" selected>每页20条</option>\n");
        html.append("<option value=\"50\">每页50条</option>\n");
        html.append("<option value=\"100\">每页100条</option>\n");
        html.append("</select>\n");
        html.append("<span style=\"color: #666; font-size: 13px;\">共 ").append(accuracy.get("totalValidationTests")).append(" 条测试用例</span>\n");
        html.append("</div>\n");
        
        html.append("<table class=\"table\" id=\"testCasesTable\">\n");
        html.append("<tr><th>类型</th><th>输入</th><th>期望输出</th><th>实际输出</th><th>结果</th></tr>\n");
        List<Map<String, String>> testCases = (List<Map<String, String>>) accuracy.get("sampleTestCases");
        for (Map<String, String> tc : testCases) {
            html.append("<tr><td>").append(tc.get("type")).append("</td>");
            html.append("<td>").append(tc.get("input")).append("</td>");
            html.append("<td>").append(tc.get("expected")).append("</td>");
            html.append("<td>").append(tc.get("actual")).append("</td>");
            html.append("<td><span class=\"status-badge ").append(tc.get("passed").equals("true") ? "status-excellent" : "status-warning").append("\">").append(tc.get("passed").equals("true") ? "通过" : "失败").append("</span></td></tr>\n");
        }
        html.append("</table>\n");
        
        html.append("<div class=\"pagination-info\" id=\"paginationInfo\">显示 1-20 条，共 ").append(accuracy.get("totalValidationTests")).append(" 条</div>\n");
        html.append("<div class=\"pagination\" id=\"pagination\"></div>\n");
        html.append("</div>\n");

        Map<String, Object> coverage = (Map<String, Object>) report.get("coverage");
        html.append("<div class=\"card\">\n");
        html.append("<h2>📊 规则覆盖率</h2>\n");
        html.append("<div class=\"metric-grid\">\n");
        html.append("<div class=\"metric-card\"><div class=\"value\">").append(coverage.get("overallCoverageRate")).append("</div><div class=\"label\">总体覆盖率</div></div>\n");
        html.append("<div class=\"metric-card\"><div class=\"value\">").append(coverage.get("coveredPatterns")).append("/").append(coverage.get("totalPatterns")).append("</div><div class=\"label\">规则覆盖数</div></div>\n");
        html.append("<div class=\"metric-card\"><div class=\"value\">").append(coverage.get("validSamplesCount")).append("</div><div class=\"label\">测试字段数</div></div>\n");
        html.append("<div class=\"metric-card\"><div class=\"value\">").append(testResultSet != null ? testResultSet.desensitizedRows : 0).append("/").append(testResultSet != null ? testResultSet.totalRows : 0).append("</div><div class=\"label\">脱敏日志行数</div></div>\n");
        html.append("</div>\n");
        
        html.append("<table class=\"table\">\n");
        html.append("<tr><th>规则</th><th>覆盖率</th><th>用例通过率</th><th>状态</th><th>描述</th></tr>\n");
        List<Map<String, Object>> ruleCoverage = (List<Map<String, Object>>) coverage.get("ruleCoverage");
        for (Map<String, Object> rc : ruleCoverage) {
            html.append("<tr><td>").append(rc.get("rule")).append("</td>");
            html.append("<td>").append(rc.get("coverageRate")).append("</td>");
            html.append("<td>").append(rc.get("testCasePassRate")).append("</td>");
            String status = (String) rc.get("status");
            String statusClass = "status-excellent";
            if ("GOOD".equals(status)) statusClass = "status-good";
            else if ("NEEDS_IMPROVEMENT".equals(status)) statusClass = "status-warning";
            else if ("DISABLED".equals(status)) statusClass = "status-warning";
            html.append("<td><span class=\"status-badge ").append(statusClass).append("\">").append(status).append("</span></td>");
            html.append("<td>").append(rc.get("description")).append("</td></tr>\n");
        }
        html.append("</table>\n");
        html.append("</div>\n");

        // 未通过用例展示版块 - 分页显示
        html.append("<div class=\"card\">\n");
        html.append("<h2>❌ 未通过用例详情</h2>\n");
        
        int totalFailed = 0;
        List<Map<String, Object>> allFailedCases = new ArrayList<>();
        for (Map<String, Object> rc : ruleCoverage) {
            List<Map<String, String>> failedCases = (List<Map<String, String>>) rc.get("failedCases");
            if (failedCases != null && !failedCases.isEmpty()) {
                totalFailed += failedCases.size();
                for (Map<String, String> fc : failedCases) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("rule", rc.get("rule"));
                    item.put("input", fc.get("input"));
                    item.put("expected", fc.get("expected"));
                    item.put("actual", fc.get("actual"));
                    allFailedCases.add(item);
                }
            }
        }
        
        if (totalFailed == 0) {
            html.append("<div style=\"text-align: center; padding: 30px; color: #28a745; font-size: 18px;\">");
            html.append("✅ 所有测试用例均已通过！");
            html.append("</div>\n");
        } else {
            html.append("<div class=\"filter-bar\">\n");
            html.append("<select id=\"failedPageSize\" onchange=\"loadFailedCases(1)\">\n");
            html.append("<option value=\"10\">每页10条</option>\n");
            html.append("<option value=\"20\" selected>每页20条</option>\n");
            html.append("<option value=\"50\">每页50条</option>\n");
            html.append("</select>\n");
            html.append("<span style=\"color: #dc3545; font-size: 14px;\">共 <strong>").append(totalFailed).append("</strong> 个未通过用例</span>\n");
            html.append("</div>\n");
            html.append("<table class=\"table\" id=\"failedCasesTable\">\n");
            html.append("<tr><th>规则类型</th><th>输入数据</th><th>期望输出</th><th>实际输出</th></tr>\n");
            int pageSize = 20;
            int displayCount = Math.min(pageSize, allFailedCases.size());
            for (int i = 0; i < displayCount; i++) {
                Map<String, Object> fc = allFailedCases.get(i);
                html.append("<tr><td>").append(fc.get("rule")).append("</td>");
                html.append("<td>").append(fc.get("input")).append("</td>");
                html.append("<td>").append(fc.get("expected")).append("</td>");
                html.append("<td style=\"color: #dc3545;\">").append(fc.get("actual")).append("</td></tr>\n");
            }
            html.append("</table>\n");
            html.append("<div class=\"pagination-info\" id=\"failedPaginationInfo\">显示 1-").append(displayCount).append(" 条，共 ").append(totalFailed).append(" 条</div>\n");
            html.append("<div class=\"pagination\" id=\"failedPagination\"></div>\n");
        }
        html.append("</div>\n");

        Map<String, Object> performance = (Map<String, Object>) report.get("performance");
        html.append("<div class=\"card\">\n");
        html.append("<h2>⚡ 性能指标 <span style=\"font-size: 13px; color: #888; font-weight: normal;\">（日志条级统计）</span></h2>\n");
        html.append("<div class=\"metric-grid\">\n");
        html.append("<div class=\"metric-card\"><div class=\"value\">").append(performance.get("totalProcessed")).append("</div><div class=\"label\">总处理量（日志条）</div><div class=\"tooltip\">从应用启动以来所有通过脱敏Appender处理的日志条数，每条日志只计1次（不论包含多少敏感字段）</div></div>\n");
        html.append("<div class=\"metric-card\"><div class=\"value\">").append(performance.getOrDefault("desensitizedLogs", 0)).append("</div><div class=\"label\">脱敏日志数</div><div class=\"tooltip\">内容被实际修改的日志条数（总处理量中真正发生了脱敏操作的日志）</div></div>\n");
        html.append("<div class=\"metric-card\"><div class=\"value\">").append(performance.get("averageTimePerRequestMs")).append("ms</div><div class=\"label\">平均耗时</div><div class=\"tooltip\">每条日志脱敏处理的平均耗时，保留4位小数精度</div></div>\n");
        html.append("<div class=\"metric-card\"><div class=\"value\">").append(performance.get("throughputPerSecond")).append("/s</div><div class=\"label\">实时吞吐量</div><div class=\"tooltip\">基于滑动窗口（最近1分钟）计算的每秒处理能力，窗口过期后显示整体平均</div></div>\n");
        html.append("<div class=\"metric-card\"><div class=\"value\">").append(performance.getOrDefault("overallThroughputPerSecond", "0.00")).append("/s</div><div class=\"label\">整体平均吞吐量</div><div class=\"tooltip\">从应用启动以来的总处理量/运行时间</div></div>\n");
        html.append("</div>\n");
        html.append("<div class=\"source-info\">统计说明: 总处理量和脱敏日志数均为日志条级统计，数据实时累计，重启应用后重置。</div>\n");
        
        html.append("<h3 style=\"margin-top: 20px; margin-bottom: 10px; color: #333; font-size: 16px;\">💡 优化建议</h3>\n");
        html.append("<ul style=\"margin-left: 20px; color: #666;\">\n");
        List<String> recommendations = (List<String>) performance.get("recommendations");
        for (String rec : recommendations) {
            html.append("<li>").append(rec).append("</li>\n");
        }
        html.append("</ul>\n");
        html.append("</div>\n");

        html.append("<div class=\"card\">\n");
        html.append("<h2>🧪 在线测试</h2>\n");
        html.append("<div class=\"test-input\">\n");
        html.append("<input type=\"text\" id=\"testText\" placeholder=\"输入要脱敏的文本...\" />\n");
        html.append("<select id=\"testType\">\n");
        html.append("<option value=\"\">自动检测</option>\n");
        html.append("<option value=\"PHONE\">PHONE</option>\n");
        html.append("<option value=\"ID_CARD\">ID_CARD</option>\n");
        html.append("<option value=\"BANK_CARD\">BANK_CARD</option>\n");
        html.append("<option value=\"EMAIL\">EMAIL</option>\n");
        html.append("<option value=\"NAME\">NAME</option>\n");
        html.append("<option value=\"ADDRESS\">ADDRESS</option>\n");
        html.append("<option value=\"PASSWORD\">PASSWORD</option>\n");
        html.append("</select>\n");
        html.append("<button onclick=\"testDesensitize()\">脱敏测试</button>\n");
        html.append("</div>\n");
        html.append("<div id=\"testResult\"></div>\n");
        html.append("</div>\n");

        html.append("<div class=\"footer\">\n");
        html.append("<p>Desensitizer Console - Java 日志脱敏工具</p>\n");
        html.append("</div>\n");

        html.append("</div>\n");

        html.append("<script>\n");
        html.append("function testDesensitize() {\n");
        html.append("var text = document.getElementById('testText').value;\n");
        html.append("var type = document.getElementById('testType').value;\n");
        html.append("if (!text) { alert('请输入要脱敏的文本'); return; }\n");
        html.append("var url = '/desensitizer/test?text=' + encodeURIComponent(text);\n");
        html.append("if (type) url += '&type=' + type;\n");
        html.append("fetch(url).then(r => r.json()).then(data => {\n");
        html.append("var result = document.getElementById('testResult');\n");
        html.append("if (data.error) {\n");
        html.append("result.innerHTML = '<div class=\"test-result\"><span style=\"color: red;\">错误: ' + data.error + '</span></div>';\n");
        html.append("} else {\n");
        html.append("result.innerHTML = '<div class=\"test-result\"><div class=\"original\">原始: ' + data.original + '</div><div class=\"desensitized\">脱敏后: ' + data.desensitized + '</div><div style=\"margin-top: 10px; font-size: 13px; color: #666;\">类型: ' + data.specifiedType + ' | 已脱敏: ' + (data.changed ? '是' : '否') + '</div></div>';\n");
        html.append("}\n");
        html.append("}).catch(e => {\n");
        html.append("document.getElementById('testResult').innerHTML = '<div class=\"test-result\"><span style=\"color: red;\">请求失败: ' + e.message + '</span></div>';\n");
        html.append("});\n");
        html.append("}\n");
        html.append("function loadTestCases(page) {\n");
        html.append("var type = document.getElementById('filterType').value;\n");
        html.append("var size = document.getElementById('pageSize').value;\n");
        html.append("var url = '/desensitizer/test/cases?page=' + page + '&size=' + size;\n");
        html.append("if (type) url += '&type=' + type;\n");
        html.append("fetch(url).then(r => r.json()).then(data => {\n");
        html.append("var table = document.getElementById('testCasesTable');\n");
        html.append("var tbody = table.tBodies[0] || table;\n");
        html.append("tbody.innerHTML = '<tr><th>类型</th><th>输入</th><th>期望输出</th><th>实际输出</th><th>结果</th></tr>';\n");
        html.append("data.content.forEach(function(tc) {\n");
        html.append("var row = '<tr><td>' + tc.type + '</td>';\n");
        html.append("row += '<td>' + tc.input + '</td>';\n");
        html.append("row += '<td>' + tc.expected + '</td>';\n");
        html.append("row += '<td>' + tc.actual + '</td>';\n");
        html.append("var statusClass = tc.passed === 'true' ? 'status-excellent' : 'status-warning';\n");
        html.append("var statusText = tc.passed === 'true' ? '通过' : '失败';\n");
        html.append("row += '<td><span class=\"status-badge ' + statusClass + '\">' + statusText + '</span></td></tr>';\n");
        html.append("tbody.innerHTML += row;\n");
        html.append("});\n");
        html.append("document.getElementById('paginationInfo').innerHTML = '显示 ' + ((data.page - 1) * data.size + 1) + '-' + Math.min(data.page * data.size, data.totalElements) + ' 条，共 ' + data.totalElements + ' 条';\n");
        html.append("renderPagination(data);\n");
        html.append("}).catch(e => {\n");
        html.append("console.error('加载测试用例失败:', e);\n");
        html.append("});\n");
        html.append("}\n");
        html.append("function renderPagination(data) {\n");
        html.append("var pagination = document.getElementById('pagination');\n");
        html.append("pagination.innerHTML = '';\n");
        html.append("var page = data.page;\n");
        html.append("var totalPages = data.totalPages;\n");
        html.append("var hasPrev = data.hasPrevious;\n");
        html.append("var hasNext = data.hasNext;\n");
        html.append("if (totalPages <= 1) return;\n");
        html.append("pagination.innerHTML += '<button ' + (hasPrev ? '' : 'disabled') + ' onclick=\"loadTestCases(' + (page - 1) + ')\">上一页</button>';\n");
        html.append("var start = Math.max(1, page - 2);\n");
        html.append("var end = Math.min(totalPages, page + 2);\n");
        html.append("if (start > 1) pagination.innerHTML += '<span style=\"color: #999;\">...</span>';\n");
        html.append("for (var i = start; i <= end; i++) {\n");
        html.append("pagination.innerHTML += '<button ' + (i === page ? 'class=\"active\"' : '') + ' onclick=\"loadTestCases(' + i + ')\">' + i + '</button>';\n");
        html.append("}\n");
        html.append("if (end < totalPages) pagination.innerHTML += '<span style=\"color: #999;\">...</span>';\n");
        html.append("pagination.innerHTML += '<button ' + (hasNext ? '' : 'disabled') + ' onclick=\"loadTestCases(' + (page + 1) + ')\">下一页</button>';\n");
        html.append("pagination.innerHTML += '<span style=\"margin-left: 10px; color: #666; font-size: 13px;\">第 ' + page + '/' + totalPages + ' 页</span>';\n");
        html.append("}\n");
        html.append("loadTestCases(1);\n");
        html.append("loadFailedCases(1);\n");
        html.append("setInterval(function() {\n");
        html.append("fetch('/desensitizer/health').then(r => r.json()).then(data => {\n");
        html.append("if (data.status !== 'UP') {\n");
        html.append("document.querySelector('.header .meta span:last-child').innerHTML = '状态: ❌ 异常';\n");
        html.append("}\n");
        html.append("});\n");
        html.append("}, 5000);\n");
        html.append("function loadFailedCases(page) {\n");
        html.append("var sizeEl = document.getElementById('failedPageSize');\n");
        html.append("if (!sizeEl) return;\n");
        html.append("var size = sizeEl.value || 20;\n");
        html.append("var url = '/desensitizer/test/failed-cases?page=' + page + '&size=' + size;\n");
        html.append("fetch(url).then(r => r.json()).then(data => {\n");
        html.append("var table = document.getElementById('failedCasesTable');\n");
        html.append("if (!table) return;\n");
        html.append("var tbody = table.tBodies[0] || table;\n");
        html.append("tbody.innerHTML = '<tr><th>规则类型</th><th>输入数据</th><th>期望输出</th><th>实际输出</th></tr>';\n");
        html.append("if (data.content && data.content.length > 0) {\n");
        html.append("data.content.forEach(function(fc) {\n");
        html.append("var row = '<tr><td>' + fc.rule + '</td>';\n");
        html.append("row += '<td>' + fc.input + '</td>';\n");
        html.append("row += '<td>' + fc.expected + '</td>';\n");
        html.append("row += '<td style=\"color: #dc3545;\">' + fc.actual + '</td></tr>';\n");
        html.append("tbody.innerHTML += row;\n");
        html.append("});\n");
        html.append("document.getElementById('failedPaginationInfo').innerHTML = '显示 ' + ((data.page - 1) * data.size + 1) + '-' + Math.min(data.page * data.size, data.totalElements) + ' 条，共 ' + data.totalElements + ' 条';\n");
        html.append("renderFailedPagination(data);\n");
        html.append("}\n");
        html.append("}).catch(e => {\n");
        html.append("console.error('加载未通过用例失败:', e);\n");
        html.append("});\n");
        html.append("}\n");
        html.append("function renderFailedPagination(data) {\n");
        html.append("var pagination = document.getElementById('failedPagination');\n");
        html.append("if (!pagination) return;\n");
        html.append("pagination.innerHTML = '';\n");
        html.append("var page = data.page;\n");
        html.append("var totalPages = data.totalPages;\n");
        html.append("var hasPrev = data.hasPrevious;\n");
        html.append("var hasNext = data.hasNext;\n");
        html.append("if (totalPages <= 1) return;\n");
        html.append("pagination.innerHTML += '<button ' + (hasPrev ? '' : 'disabled') + ' onclick=\"loadFailedCases(' + (page - 1) + ')\">上一页</button>';\n");
        html.append("var start = Math.max(1, page - 2);\n");
        html.append("var end = Math.min(totalPages, page + 2);\n");
        html.append("if (start > 1) pagination.innerHTML += '<span style=\"color: #999;\">...</span>';\n");
        html.append("for (var i = start; i <= end; i++) {\n");
        html.append("pagination.innerHTML += '<button ' + (i === page ? 'class=\"active\"' : '') + ' onclick=\"loadFailedCases(' + i + ')\">' + i + '</button>';\n");
        html.append("}\n");
        html.append("if (end < totalPages) pagination.innerHTML += '<span style=\"color: #999;\">...</span>';\n");
        html.append("pagination.innerHTML += '<button ' + (hasNext ? '' : 'disabled') + ' onclick=\"loadFailedCases(' + (page + 1) + ')\">下一页</button>';\n");
        html.append("pagination.innerHTML += '<span style=\"margin-left: 10px; color: #666; font-size: 13px;\">第 ' + page + '/' + totalPages + ' 页</span>';\n");
        html.append("}\n");
        html.append("</script>\n");

        html.append("</body>\n</html>");

        return html.toString();
    }

}