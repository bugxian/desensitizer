package com.desensitizer.core.handler;

import com.desensitizer.core.api.Desensitizer;
import com.desensitizer.core.api.SensitiveType;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringTypeHandler {

    private final Map<SensitiveType, Desensitizer> desensitizerMap;
    private final Map<SensitiveType, Pattern> patternMap;
    private final Map<SensitiveType, Integer> groupIndexMap;

    public StringTypeHandler(Map<SensitiveType, Desensitizer> desensitizerMap) {
        this.desensitizerMap = desensitizerMap;
        this.patternMap = new HashMap<>();
        this.groupIndexMap = new HashMap<>();
        initPatterns();
    }

    private void initPatterns() {
        try {
            patternMap.put(SensitiveType.PHONE, Pattern.compile("1[3-9]\\d{9}"));
            groupIndexMap.put(SensitiveType.PHONE, 0);

            patternMap.put(SensitiveType.ID_CARD, Pattern.compile("\\d{17}[\\dXx]|\\d{15}"));
            groupIndexMap.put(SensitiveType.ID_CARD, 0);

            patternMap.put(SensitiveType.NAME, Pattern.compile("(?<=name|姓名|联系人|用户)[=:：\\s]*([\\u4e00-\\u9fa5]{2,4})"));
            groupIndexMap.put(SensitiveType.NAME, 1);

            patternMap.put(SensitiveType.ADDRESS, Pattern.compile(
                    "(?:地址[：:]\\s*|addr(?:ess)?[\\s:=]+)([^，,\\s]{5,})|"
                    + "([\\u4e00-\\u9fa5]{2,}(?:省|自治区|直辖市))?[\\u4e00-\\u9fa5]{2,}(?:市|自治州)[\\u4e00-\\u9fa5]{2,}(?:区|县|旗)[\\u4e00-\\u9fa5a-zA-Z0-9\\s-]*[\\d座栋号室]?"
            ));
            groupIndexMap.put(SensitiveType.ADDRESS, 0);
        } catch (Exception e) {
            System.err.println("[StringTypeHandler] 正则表达式初始化异常: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    public String handle(String content, SensitiveType type) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        try {
            if (type == null) {
                return autoDesensitize(content);
            }
            Desensitizer desensitizer = desensitizerMap.get(type);
            if (desensitizer == null) {
                return content;
            }
            return applyStrategy(content, desensitizer, type);
        } catch (Exception e) {
            System.err.println("[StringTypeHandler] 字符串脱敏异常: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            return content;
        }
    }

    public String handle(String content) {
        try {
            return autoDesensitize(content);
        } catch (Exception e) {
            System.err.println("[StringTypeHandler] 字符串自动脱敏异常: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            return content;
        }
    }

    private String autoDesensitize(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        String result = content;
        for (Map.Entry<SensitiveType, Pattern> entry : patternMap.entrySet()) {
            try {
                SensitiveType type = entry.getKey();
                Pattern pattern = entry.getValue();
                Desensitizer desensitizer = desensitizerMap.get(type);
                if (desensitizer == null) {
                    continue;
                }
                result = applyPattern(result, pattern, desensitizer, groupIndexMap.getOrDefault(type, 0));
            } catch (Exception e) {
                System.err.println("[StringTypeHandler] 正则匹配脱敏异常: type=" + entry.getKey()
                        + " - " + e.getClass().getSimpleName() + " - " + e.getMessage());
            }
        }
        return result;
    }

    private String applyStrategy(String content, Desensitizer desensitizer, SensitiveType type) {
        try {
            Pattern pattern = patternMap.get(type);
            if (pattern != null) {
                return applyPattern(content, pattern, desensitizer, groupIndexMap.getOrDefault(type, 0));
            }
            return desensitizer.desensitize(content);
        } catch (Exception e) {
            System.err.println("[StringTypeHandler] 策略应用异常: type=" + type
                    + " - " + e.getClass().getSimpleName() + " - " + e.getMessage());
            return content;
        }
    }

    private String applyPattern(String content, Pattern pattern, Desensitizer desensitizer, int groupIndex) {
        try {
            Matcher matcher = pattern.matcher(content);
            StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                String matched = matcher.group(groupIndex);
                String masked = desensitizer.desensitize(matched);
                if (groupIndex > 0) {
                    String fullMatch = matcher.group(0);
                    String replacement = fullMatch.replace(matched, masked);
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
                } else {
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(masked));
                }
            }
            matcher.appendTail(sb);
            return sb.toString();
        } catch (Exception e) {
            System.err.println("[StringTypeHandler] 正则替换异常: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            return content;
        }
    }
}