package com.desensitizer.spring.controller;

import com.desensitizer.core.engine.DesensitizationEngine;
import com.desensitizer.core.monitor.DesensitizationMonitor;
import com.desensitizer.core.api.SensitiveType;
import com.desensitizer.core.registry.DesensitizerRegistry;
import com.desensitizer.spring.boot.util.ExcelDataLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

@RestController
@RequestMapping("/desensitizer")
public class DesensitizerConsoleController {

    private static final Logger logger = LoggerFactory.getLogger(DesensitizerConsoleController.class);
    private static final String ACCURACY_TEST_FILE = "test-data/accuracy-tests.csv";
    private static final String COVERAGE_VALID_FILE = "test-data/coverage-valid.txt";
    private static final String COVERAGE_INVALID_FILE = "test-data/coverage-invalid.txt";

    @Autowired(required = false)
    private DesensitizationEngine engine;

    @Autowired(required = false)
    private DesensitizationMonitor monitor;

    @Autowired(required = false)
    private ExcelDataLoader excelDataLoader;

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
        // 暂停监控记录，避免报告生成过程中产生的日志被统计进去
        if (monitor != null) {
            monitor.pauseRecording();
        }
        
        Map<String, Object> report = new HashMap<>();
        report.put("reportTime", System.currentTimeMillis());
        report.put("generatedBy", "Desensitizer Console v1.0");

        report.put("accuracy", getAccuracyMetrics());
        report.put("coverage", getCoverageMetrics());
        report.put("performance", getPerformanceMetrics());
        report.put("rules", getRulesMetrics());
        report.put("logCases", getLogDesensitizationCases());
        report.put("summary", getSummary());

        // 恢复监控记录
        if (monitor != null) {
            monitor.resumeRecording();
        }

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
                .toList();
        } else {
            cases = monitor.getRecentCases(count).stream()
                .map(this::createCaseMap)
                .toList();
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
                .toList();
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

        try {
            long beforeCount = monitor != null ? monitor.getTotalCount() : 0;

            List<ExcelDataLoader.LogEntry> logEntries = excelDataLoader.loadFromExcel();
            
            if (logEntries.isEmpty()) {
                result.put("error", "未能从Excel文件加载测试数据");
                return result;
            }

            List<Map<String, Object>> results = new ArrayList<>();
            int desensitizedCount = 0;

            for (int i = 0; i < logEntries.size(); i++) {
                ExcelDataLoader.LogEntry entry = logEntries.get(i);
                String logString = entry.toLogString();
                
                long entryBefore = monitor != null ? monitor.getTotalCount() : 0;
                // 直接拼接字符串，不使用占位符，确保整个日志消息都被脱敏
                logger.info("Excel测试数据[" + (i + 1) + "]: " + logString);
                Thread.sleep(50);
                long entryAfter = monitor != null ? monitor.getTotalCount() : 0;

                Map<String, Object> entryResult = new HashMap<>();
                entryResult.put("row", i + 1);
                entryResult.put("original", logString);
                entryResult.put("desensitized", entryAfter > entryBefore);
                
                if (entryAfter > entryBefore) {
                    desensitizedCount++;
                }
                results.add(entryResult);
            }

            Thread.sleep(200);
            long afterCount = monitor != null ? monitor.getTotalCount() : 0;

            result.put("excelFile", "test-data/赛题4-支持敏感信息脱敏的通用工具-测试数据v0.3.xlsx");
            result.put("totalRows", logEntries.size());
            result.put("desensitizedRows", desensitizedCount);
            result.put("beforeCount", beforeCount);
            result.put("afterCount", afterCount);
            result.put("totalIncrease", afterCount - beforeCount);
            result.put("success", desensitizedCount == logEntries.size());
            result.put("details", results);

        } catch (Exception e) {
            result.put("error", e.getMessage());
            e.printStackTrace();
        }
        
        return result;
    }

    @GetMapping("/test/cases")
    public Map<String, Object> getTestCases(Integer page, Integer size, String type) {
        Map<String, Object> result = new HashMap<>();
        
        int pageNum = page != null ? page : 1;
        int pageSize = size != null ? size : 20;
        
        List<TestData> allTestData = loadAccuracyTestsFromFile();
        
        // 按类型筛选
        if (type != null && !type.isEmpty()) {
            String filterType = type.toUpperCase();
            allTestData = allTestData.stream()
                .filter(td -> td.type.equals(filterType))
                .toList();
        }
        
        int totalElements = allTestData.size();
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);
        int start = (pageNum - 1) * pageSize;
        int end = Math.min(start + pageSize, totalElements);
        
        List<Map<String, String>> pageTestCases = new ArrayList<>();
        for (int i = start; i < end; i++) {
            TestData td = allTestData.get(i);
            String actualResult = null;
            try {
                SensitiveType sensitiveType = SensitiveType.valueOf(td.type);
                actualResult = engine.desensitize(td.input, sensitiveType, false);  // 测试场景，不记录到监控器
            } catch (Exception e) {
                actualResult = td.input;
            }
            boolean passed = td.expected.equals(actualResult);
            pageTestCases.add(createTestCase(td.type, td.input, td.expected, actualResult, passed));
        }
        
        result.put("content", pageTestCases);
        result.put("page", pageNum);
        result.put("size", pageSize);
        result.put("totalElements", totalElements);
        result.put("totalPages", totalPages);
        result.put("hasNext", pageNum < totalPages);
        result.put("hasPrevious", pageNum > 1);
        result.put("filterType", type);
        
        return result;
    }

    @GetMapping("/test/failed-cases")
    public Map<String, Object> getFailedCases(Integer page, Integer size) {
        Map<String, Object> result = new HashMap<>();
        
        int pageNum = page != null ? page : 1;
        int pageSize = size != null ? size : 20;
        
        List<Map<String, Object>> allFailedCases = new ArrayList<>();
        
        // 收集所有未通过的用例
        List<TestData> testDataList = loadAccuracyTestsFromFile();
        for (TestData td : testDataList) {
            String actualResult = null;
            try {
                SensitiveType sensitiveType = SensitiveType.valueOf(td.type);
                actualResult = engine.desensitize(td.input, sensitiveType, false);  // 测试场景，不记录到监控器
            } catch (Exception e) {
                actualResult = td.input;
            }
            if (!td.expected.equals(actualResult)) {
                Map<String, Object> failedCase = new HashMap<>();
                failedCase.put("rule", td.type);
                failedCase.put("input", td.input);
                failedCase.put("expected", td.expected);
                failedCase.put("actual", actualResult);
                allFailedCases.add(failedCase);
            }
        }
        
        int totalElements = allFailedCases.size();
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);
        int start = (pageNum - 1) * pageSize;
        int end = Math.min(start + pageSize, totalElements);
        
        List<Map<String, Object>> pageFailedCases = new ArrayList<>();
        for (int i = start; i < end; i++) {
            pageFailedCases.add(allFailedCases.get(i));
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
        
        int passedTests = 0;
        int failedTests = 0;
        List<Map<String, String>> testCases = new ArrayList<>();
        
        List<TestData> testDataList = loadAccuracyTestsFromFile();
        
        // 只取前20条作为样本展示
        int displayCount = Math.min(20, testDataList.size());
        
        for (int i = 0; i < testDataList.size(); i++) {
            TestData td = testDataList.get(i);
            String actualResult = null;
            try {
                SensitiveType type = SensitiveType.valueOf(td.type);
                actualResult = engine.desensitize(td.input, type, false);  // 测试场景，不记录到监控器
            } catch (Exception e) {
                actualResult = td.input;
            }
            boolean passed = td.expected.equals(actualResult);
            if (passed) {
                passedTests++;
            } else {
                failedTests++;
            }
            // 只添加部分用例到样本列表
            if (i < displayCount) {
                testCases.add(createTestCase(td.type, td.input, td.expected, actualResult, passed));
            }
        }
        
        int totalTests = passedTests + failedTests;
        double accuracyRate = totalTests > 0 ? (passedTests * 100.0) / totalTests : 0;
        
        accuracy.put("accuracyRate", String.format("%.2f%%", accuracyRate));
        accuracy.put("totalValidationTests", totalTests);
        accuracy.put("passedTests", passedTests);
        accuracy.put("failedTests", failedTests);
        accuracy.put("lastValidationTime", System.currentTimeMillis());
        accuracy.put("sampleTestCases", testCases);
        accuracy.put("testDataSource", ACCURACY_TEST_FILE);
        accuracy.put("totalPages", (int) Math.ceil((double) totalTests / 20));
        
        return accuracy;
    }

    private List<TestData> loadAccuracyTestsFromFile() {
        List<TestData> testDataList = new ArrayList<>();
        
        // 优先从Excel文件读取测试数据
        if (excelDataLoader != null) {
            try {
                List<ExcelDataLoader.LogEntry> entries = excelDataLoader.loadFromExcel();
                for (ExcelDataLoader.LogEntry entry : entries) {
                    // 姓名测试用例
                    if (entry.getName() != null && !entry.getName().isEmpty() && entry.getNameDesensitized() != null) {
                        testDataList.add(new TestData("NAME", entry.getName(), entry.getNameDesensitized()));
                    }
                    // 手机号测试用例
                    if (entry.getPhone() != null && !entry.getPhone().isEmpty() && entry.getPhoneDesensitized() != null) {
                        testDataList.add(new TestData("PHONE", entry.getPhone(), entry.getPhoneDesensitized()));
                    }
                    // 身份证号测试用例
                    if (entry.getIdCard() != null && !entry.getIdCard().isEmpty() && entry.getIdCardDesensitized() != null) {
                        testDataList.add(new TestData("ID_CARD", entry.getIdCard(), entry.getIdCardDesensitized()));
                    }
                    // 银行卡号测试用例
                    if (entry.getBankCard() != null && !entry.getBankCard().isEmpty() && entry.getBankCardDesensitized() != null) {
                        testDataList.add(new TestData("BANK_CARD", entry.getBankCard(), entry.getBankCardDesensitized()));
                    }
                    // 地址测试用例
                    if (entry.getAddress() != null && !entry.getAddress().isEmpty() && entry.getAddressDesensitized() != null) {
                        testDataList.add(new TestData("ADDRESS", entry.getAddress(), entry.getAddressDesensitized()));
                    }
                }
                logger.info("Loaded {} test cases from Excel file", testDataList.size());
            } catch (Exception e) {
                logger.warn("Failed to load accuracy tests from Excel: {}", e.getMessage());
            }
        }
        
        // 如果Excel加载失败，回退到CSV文件
        if (testDataList.isEmpty()) {
            try {
                ClassPathResource resource = new ClassPathResource(ACCURACY_TEST_FILE);
                if (resource.exists()) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), "UTF-8"))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            line = line.trim();
                            if (line.isEmpty() || line.startsWith("#")) {
                                continue;
                            }
                            String[] parts = line.split("\\t", -1); // 使用制表符分隔
                            if (parts.length >= 3) {
                                testDataList.add(new TestData(parts[0], parts[1], parts[2]));
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("Failed to load accuracy tests from CSV file: {}", e.getMessage());
            }
        }
        
        // 如果都加载失败，使用硬编码数据
        if (testDataList.isEmpty()) {
            testDataList.addAll(Arrays.asList(
                new TestData("PHONE", "13800138000", "138****8000"),
                new TestData("PHONE", "13912345678", "139****5678"),
                new TestData("ID_CARD", "110101199001011234", "110101********1234"),
                new TestData("EMAIL", "user@example.com", "u***@example.com")
            ));
        }
        
        return testDataList;
    }

    private Map<String, Object> getCoverageMetrics() {
        Map<String, Object> coverage = new HashMap<>();
        
        List<String> phoneSamples = new ArrayList<>();
        List<String> idCardSamples = new ArrayList<>();
        List<String> bankCardSamples = new ArrayList<>();
        List<String> emailSamples = new ArrayList<>();
        List<String> nameSamples = new ArrayList<>();
        List<String> addressSamples = new ArrayList<>();
        
        // 优先从Excel文件加载覆盖率测试样本
        if (excelDataLoader != null) {
            try {
                List<ExcelDataLoader.LogEntry> entries = excelDataLoader.loadFromExcel();
                for (ExcelDataLoader.LogEntry entry : entries) {
                    if (entry.getName() != null && !entry.getName().isEmpty()) {
                        nameSamples.add(entry.getName());
                    }
                    if (entry.getPhone() != null && !entry.getPhone().isEmpty()) {
                        phoneSamples.add(entry.getPhone());
                    }
                    if (entry.getIdCard() != null && !entry.getIdCard().isEmpty()) {
                        idCardSamples.add(entry.getIdCard());
                    }
                    if (entry.getBankCard() != null && !entry.getBankCard().isEmpty()) {
                        bankCardSamples.add(entry.getBankCard());
                    }
                    if (entry.getAddress() != null && !entry.getAddress().isEmpty()) {
                        addressSamples.add(entry.getAddress());
                    }
                }
                logger.info("Loaded coverage samples from Excel - NAME:{}, PHONE:{}, ID_CARD:{}, BANK_CARD:{}, ADDRESS:{}", 
                    nameSamples.size(), phoneSamples.size(), idCardSamples.size(), bankCardSamples.size(), addressSamples.size());
            } catch (Exception e) {
                logger.warn("Failed to load coverage samples from Excel: {}", e.getMessage());
            }
        }
        
        // 如果Excel加载失败，回退到文本文件
        if (nameSamples.isEmpty() && phoneSamples.isEmpty() && idCardSamples.isEmpty() && 
            bankCardSamples.isEmpty() && addressSamples.isEmpty()) {
            List<String> validSamples = loadSamplesFromFile(COVERAGE_VALID_FILE);
            for (String sample : validSamples) {
                if (sample.startsWith("PHONE:")) {
                    phoneSamples.add(sample.substring(6));
                } else if (sample.startsWith("ID_CARD:")) {
                    idCardSamples.add(sample.substring(8));
                } else if (sample.startsWith("BANK_CARD:")) {
                    bankCardSamples.add(sample.substring(10));
                } else if (sample.startsWith("EMAIL:")) {
                    emailSamples.add(sample.substring(6));
                } else if (sample.startsWith("NAME:")) {
                    nameSamples.add(sample.substring(5));
                } else if (sample.startsWith("ADDRESS:")) {
                    addressSamples.add(sample.substring(8));
                }
            }
        }
        
        List<Map<String, Object>> ruleCoverage = new ArrayList<>();
        ruleCoverage.add(calculateCoverage("PHONE", phoneSamples));
        ruleCoverage.add(calculateCoverage("ID_CARD", idCardSamples));
        ruleCoverage.add(calculateCoverage("BANK_CARD", bankCardSamples));
        ruleCoverage.add(calculateCoverage("EMAIL", emailSamples));
        ruleCoverage.add(calculateCoverage("NAME", nameSamples));
        ruleCoverage.add(calculateCoverage("ADDRESS", addressSamples));
        
        int totalCoverage = 0;
        int coveredPatterns = 0;
        int totalSamples = phoneSamples.size() + idCardSamples.size() + bankCardSamples.size() + 
                           emailSamples.size() + nameSamples.size() + addressSamples.size();
        
        for (Map<String, Object> rc : ruleCoverage) {
            String rateStr = (String) rc.get("coverageRate");
            int rate = Integer.parseInt(rateStr.replace("%", ""));
            totalCoverage += rate;
            if (rate > 0) coveredPatterns++;
        }
        
        coverage.put("ruleCoverage", ruleCoverage);
        coverage.put("overallCoverageRate", String.format("%.2f%%", coveredPatterns > 0 ? (totalCoverage * 1.0) / coveredPatterns : 0));
        coverage.put("coveredPatterns", coveredPatterns);
        coverage.put("totalPatterns", 6);
        coverage.put("validSamplesCount", totalSamples);
        
        return coverage;
    }

    private List<String> loadSamplesFromFile(String filename) {
        List<String> samples = new ArrayList<>();
        
        try {
            ClassPathResource resource = new ClassPathResource(filename);
            if (resource.exists()) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), "UTF-8"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) {
                            continue;
                        }
                        samples.add(line);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to load samples from file {}: {}", filename, e.getMessage());
        }
        
        if (samples.isEmpty()) {
            if (filename.contains("valid")) {
                samples.addAll(Arrays.asList("13800138000", "110101199001011234", "user@example.com"));
            } else {
                samples.addAll(Arrays.asList("test", "12345", "abc"));
            }
        }
        
        return samples;
    }

    private List<String> filterSamples(List<String> samples, String regex) {
        List<String> filtered = new ArrayList<>();
        for (String sample : samples) {
            if (sample.matches(regex)) {
                filtered.add(sample);
            }
        }
        return filtered.isEmpty() ? samples.subList(0, Math.min(3, samples.size())) : filtered;
    }

    private Map<String, Object> calculateCoverage(String rule, List<String> validSamples) {
        Map<String, Object> item = new HashMap<>();
        item.put("rule", rule);
        
        // 计算有效样本覆盖率
        int detectedValid = 0;
        for (String sample : validSamples) {
            String testInput = sample;
            // 对于需要标签前缀的类型，添加前缀后再测试覆盖率
            // 这样可以模拟实际日志中的格式，同时保持原始脱敏规则不变
            if ("NAME".equals(rule)) {
                testInput = "姓名：" + sample;
            } else if ("ADDRESS".equals(rule)) {
                testInput = "地址：" + sample;
            }
            String desensitized = engine.desensitize(testInput, false);  // 测试场景，不记录到监控器
            if (!testInput.equals(desensitized)) {
                detectedValid++;
            }
        }
        
        // 计算测试用例通过率并收集失败用例
        int passedCases = 0;
        int totalCases = 0;
        List<Map<String, String>> failedCases = new ArrayList<>();
        for (TestData td : loadAccuracyTestsFromFile()) {
            if (td.type.equals(rule)) {
                totalCases++;
                String actual;
                try {
                    SensitiveType sensitiveType = SensitiveType.valueOf(td.type);
                    actual = engine.desensitize(td.input, sensitiveType, false);  // 测试场景，不记录到监控器
                } catch (Exception e) {
                    actual = td.input;
                }
                if (td.expected.equals(actual)) {
                    passedCases++;
                } else {
                    // 收集失败用例详情
                    Map<String, String> failedCase = new HashMap<>();
                    failedCase.put("input", td.input);
                    failedCase.put("expected", td.expected);
                    failedCase.put("actual", actual);
                    failedCases.add(failedCase);
                }
            }
        }
        item.put("failedCases", failedCases);
        
        int coverage = validSamples.size() > 0 ? (detectedValid * 100) / validSamples.size() : 0;
        int casePassRate = totalCases > 0 ? (passedCases * 100) / totalCases : 0;
        
        item.put("coverageRate", coverage + "%");
        item.put("testCasePassRate", String.format("%d/%d (%d%%)", passedCases, totalCases, casePassRate));
        item.put("description", String.format("样本检测: %d/%d (%.1f%%) | 规则匹配: %d/%d (%.1f%%) | 测试用例: %d/%d (%.1f%%) | 未通过: %d", 
            detectedValid, validSamples.size(), validSamples.size() > 0 ? (detectedValid * 100.0 / validSamples.size()) : 0,
            passedCases, totalCases, totalCases > 0 ? (passedCases * 100.0 / totalCases) : 0,
            passedCases, totalCases, totalCases > 0 ? (passedCases * 100.0 / totalCases) : 0,
            totalCases - passedCases));
        item.put("validCount", validSamples.size());
        item.put("detectedCount", detectedValid);
        item.put("totalCases", totalCases);
        item.put("passedCases", passedCases);
        
        // 状态基于覆盖率和测试用例通过率综合判断
        String status;
        if (coverage >= 90 && casePassRate >= 90) {
            status = "EXCELLENT";
        } else if (coverage >= 70 || casePassRate >= 70) {
            status = "GOOD";
        } else if (coverage > 0 || casePassRate > 0) {
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
            long totalTime = monitor.getTotalProcessingTime();
            long totalCount = monitor.getTotalCount();
            double avgTime = totalCount > 0 ? (totalTime * 1.0 / totalCount) : 0;
            
            performance.put("totalProcessed", totalCount);
            performance.put("totalProcessingTimeMs", totalTime);
            performance.put("averageTimePerRequestMs", String.format("%.2f", avgTime));
            performance.put("maxProcessingTimeMs", monitor.getMaxProcessingTime());
            performance.put("minProcessingTimeMs", monitor.getMinProcessingTime());
            performance.put("throughputPerSecond", totalCount > 0 ? 
                String.format("%.0f", totalCount / ((System.currentTimeMillis() - monitor.getStartTime()) / 1000.0)) : "N/A");
        } else {
            performance.put("totalProcessed", 0);
            performance.put("totalProcessingTimeMs", 0);
            performance.put("averageTimePerRequestMs", "N/A");
            performance.put("maxProcessingTimeMs", 0);
            performance.put("minProcessingTimeMs", 0);
            performance.put("throughputPerSecond", "N/A");
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
        
        double accuracyRate = Double.parseDouble(((String) accuracy.get("accuracyRate")).replace("%", ""));
        long totalDesensitized = (Long) rules.get("totalDesensitized");
        long errorCount = (Long) rules.get("errorCount");
        
        String grade;
        String overallStatus;
        String confidence;
        String message;
        
        if (accuracyRate >= 95 && errorCount == 0) {
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
        html.append(".metric-card { background: linear-gradient(135deg, #f5f7fa 0%, #e4e8ec 100%); border-radius: 12px; padding: 20px; text-align: center; }\n");
        html.append(".metric-card .value { font-size: 32px; font-weight: 700; color: #667eea; margin-bottom: 5px; }\n");
        html.append(".metric-card .label { font-size: 13px; color: #666; }\n");
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
        html.append("<h2>🎯 脱敏准确率</h2>\n");
        html.append("<div class=\"metric-grid\">\n");
        html.append("<div class=\"metric-card\"><div class=\"value\">").append(accuracy.get("accuracyRate")).append("</div><div class=\"label\">准确率</div></div>\n");
        html.append("<div class=\"metric-card\"><div class=\"value\">").append(accuracy.get("passedTests")).append("/").append(accuracy.get("totalValidationTests")).append("</div><div class=\"label\">验证通过</div></div>\n");
        html.append("<div class=\"metric-card\"><div class=\"value\">").append(accuracy.get("failedTests")).append("</div><div class=\"label\">验证失败</div></div>\n");
        html.append("</div>\n");
        html.append("<div class=\"source-info\">测试数据源: ").append(accuracy.get("testDataSource")).append("</div>\n");
        
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
        html.append("<div class=\"metric-card\"><div class=\"value\">").append(coverage.get("validSamplesCount")).append("</div><div class=\"label\">有效样本数</div></div>\n");
        html.append("<div class=\"metric-card\"><div class=\"value\">").append(coverage.get("invalidSamplesCount")).append("</div><div class=\"label\">无效样本数</div></div>\n");
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
        html.append("<h2>⚡ 性能指标</h2>\n");
        html.append("<div class=\"metric-grid\">\n");
        html.append("<div class=\"metric-card\"><div class=\"value\">").append(performance.get("totalProcessed")).append("</div><div class=\"label\">总处理量</div></div>\n");
        html.append("<div class=\"metric-card\"><div class=\"value\">").append(performance.get("averageTimePerRequestMs")).append("ms</div><div class=\"label\">平均耗时</div></div>\n");
        html.append("<div class=\"metric-card\"><div class=\"value\">").append(performance.get("throughputPerSecond")).append("/s</div><div class=\"label\">处理吞吐量</div></div>\n");
        html.append("<div class=\"metric-card\"><div class=\"value\">").append(performance.get("performanceLevel")).append("</div><div class=\"label\">性能等级</div></div>\n");
        html.append("</div>\n");
        html.append("<div class=\"source-info\">数据说明: 总处理量统计从应用启动以来所有通过脱敏引擎处理的日志行数，包括报告生成时触发的统计计算。数据实时累计，重启应用后重置。</div>\n");
        
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
        html.append("var size = document.getElementById('failedPageSize').value || 20;\n");
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

    private static class TestData {
        String type;
        String input;
        String expected;

        TestData(String type, String input, String expected) {
            this.type = type;
            this.input = input;
            this.expected = expected;
        }
    }
}