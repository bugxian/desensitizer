package com.desensitizer.spring;

import com.desensitizer.core.api.SensitiveType;
import com.desensitizer.core.detector.RegexDetector;
import com.desensitizer.core.engine.DesensitizationEngine;
import com.desensitizer.core.registry.DesensitizerRegistry;
import com.desensitizer.builtin.desensitizer.*;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "desensitizer")
public class DesensitizerProperties {

    private boolean enabled = true;
    private RegexConfig regex = new RegexConfig();
    private LogConfig log = new LogConfig();
    private AppenderConfig appenders = new AppenderConfig();
    private RulesConfig rules = new RulesConfig();
    private MonitoringConfig monitoring = new MonitoringConfig();
    private JsonConfig json = new JsonConfig();
    private ErrorLogConfig errorLog = new ErrorLogConfig();
    private NlpConfig nlp = new NlpConfig();
    private String customPackages;

    public static class LogConfig {
        private boolean enabled = true;
        private boolean logback = true;
        private boolean log4j2 = true;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public boolean isLogback() { return logback; }
        public void setLogback(boolean logback) { this.logback = logback; }
        public boolean isLog4j2() { return log4j2; }
        public void setLog4j2(boolean log4j2) { this.log4j2 = log4j2; }
    }

    public static class AppenderConfig {
        private List<String> include = new ArrayList<>();
        private List<String> exclude = new ArrayList<>();
        private List<CustomAppenderConfig> custom = new ArrayList<>();

        public List<String> getInclude() { return include; }
        public void setInclude(List<String> include) { this.include = include; }
        public List<String> getExclude() { return exclude; }
        public void setExclude(List<String> exclude) { this.exclude = exclude; }
        public List<CustomAppenderConfig> getCustom() { return custom; }
        public void setCustom(List<CustomAppenderConfig> custom) { this.custom = custom; }
    }

    public static class CustomAppenderConfig {
        private String name;
        private List<String> patterns = new ArrayList<>();
        private boolean enabled = true;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public List<String> getPatterns() { return patterns; }
        public void setPatterns(List<String> patterns) { this.patterns = patterns; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    public static class RulesConfig {
        private Map<String, AppenderRulesConfig> appSpecific = new HashMap<>();

        public Map<String, AppenderRulesConfig> getAppSpecific() { return appSpecific; }
        public void setAppSpecific(Map<String, AppenderRulesConfig> appSpecific) { this.appSpecific = appSpecific; }
    }

    public static class AppenderRulesConfig {
        private boolean enabled = true;
        private List<SensitiveType> types = new ArrayList<>();

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public List<SensitiveType> getTypes() { return types; }
        public void setTypes(List<SensitiveType> types) { this.types = types; }
    }

    public static class MonitoringConfig {
        private boolean enabled = true;
        private StatsConfig stats = new StatsConfig();
        private ErrorsConfig errors = new ErrorsConfig();

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public StatsConfig getStats() { return stats; }
        public void setStats(StatsConfig stats) { this.stats = stats; }
        public ErrorsConfig getErrors() { return errors; }
        public void setErrors(ErrorsConfig errors) { this.errors = errors; }
    }

    public static class StatsConfig {
        private boolean enabled = true;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    public static class ErrorsConfig {
        private boolean enabled = true;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    public static class RegexConfig {
        private boolean enabled = true;
        private String phonePattern = "1[3-9]\\d{9}";
        private String idCardPattern = "[1-9]\\d{5}\\d{4}\\d{4}\\d{3}[\\dXx]";
        private String bankCardPattern = "[621789]\\d{15,19}";
        private String emailPattern = "\\w+@\\w+\\.\\w+";
        private String passwordPattern = "(?i)(?:password|pwd|密码)\\s*[:=]\\s*(\\S+)";
        private String namePattern = "姓名[：:]([^，,\\s]+(?:\\s+[^，,\\s]+){0,2})";
        private String addressPattern = "([\\u4e00-\\u9fa5]{2,}(?:省|市|县|区|镇|乡|村|街|路|道|巷|座|栋|单元|室)[\\u4e00-\\u9fa5a-zA-Z0-9\\s-]*[\\d座栋号室]?)|(\\d+\\s+[A-Za-z]+(?:\\s+[A-Za-z]+)*(?:,\\s*[A-Za-z\\s]+)*\\s*\\d{4,6})";
        private String addressKeyPattern = "地址[：:]([^，,]+)";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getPhonePattern() { return phonePattern; }
        public void setPhonePattern(String phonePattern) { this.phonePattern = phonePattern; }
        public String getIdCardPattern() { return idCardPattern; }
        public void setIdCardPattern(String idCardPattern) { this.idCardPattern = idCardPattern; }
        public String getBankCardPattern() { return bankCardPattern; }
        public void setBankCardPattern(String bankCardPattern) { this.bankCardPattern = bankCardPattern; }
        public String getEmailPattern() { return emailPattern; }
        public void setEmailPattern(String emailPattern) { this.emailPattern = emailPattern; }
        public String getPasswordPattern() { return passwordPattern; }
        public void setPasswordPattern(String passwordPattern) { this.passwordPattern = passwordPattern; }
        public String getNamePattern() { return namePattern; }
        public void setNamePattern(String namePattern) { this.namePattern = namePattern; }
        public String getAddressPattern() { return addressPattern; }
        public void setAddressPattern(String addressPattern) { this.addressPattern = addressPattern; }
        public String getAddressKeyPattern() { return addressKeyPattern; }
        public void setAddressKeyPattern(String addressKeyPattern) { this.addressKeyPattern = addressKeyPattern; }
    }

    public static class JsonConfig {
        private boolean enabled = true;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    public static class ErrorLogConfig {
        private boolean enabled = true;
        private String path = "logs/desensitizer-error.log";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
    }

    public static class NlpConfig {
        private boolean enabled = false;
        private String modelPath = "classpath:/nlp-model/albert_tiny/";
        private String provider = "onnx";
        private float confidenceThreshold = 0.8f;
        private int batchSize = 32;
        private int timeout = 100;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getModelPath() { return modelPath; }
        public void setModelPath(String modelPath) { this.modelPath = modelPath; }
        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        public float getConfidenceThreshold() { return confidenceThreshold; }
        public void setConfidenceThreshold(float confidenceThreshold) { this.confidenceThreshold = confidenceThreshold; }
        public int getBatchSize() { return batchSize; }
        public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
        public int getTimeout() { return timeout; }
        public void setTimeout(int timeout) { this.timeout = timeout; }
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public RegexConfig getRegex() { return regex; }
    public void setRegex(RegexConfig regex) { this.regex = regex; }
    public LogConfig getLog() { return log; }
    public void setLog(LogConfig log) { this.log = log; }
    public AppenderConfig getAppenders() { return appenders; }
    public void setAppenders(AppenderConfig appenders) { this.appenders = appenders; }
    public RulesConfig getRules() { return rules; }
    public void setRules(RulesConfig rules) { this.rules = rules; }
    public MonitoringConfig getMonitoring() { return monitoring; }
    public void setMonitoring(MonitoringConfig monitoring) { this.monitoring = monitoring; }
    public JsonConfig getJson() { return json; }
    public void setJson(JsonConfig json) { this.json = json; }
    public ErrorLogConfig getErrorLog() { return errorLog; }
    public void setErrorLog(ErrorLogConfig errorLog) { this.errorLog = errorLog; }
    public NlpConfig getNlp() { return nlp; }
    public void setNlp(NlpConfig nlp) { this.nlp = nlp; }
    public String getCustomPackages() { return customPackages; }
    public void setCustomPackages(String customPackages) { this.customPackages = customPackages; }
}
