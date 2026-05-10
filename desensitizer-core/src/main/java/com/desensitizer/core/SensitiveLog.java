package com.desensitizer.core;

import com.desensitizer.core.api.Desensitizer;
import com.desensitizer.core.api.SensitiveType;
import com.desensitizer.core.config.SensitiveConfig;
import com.desensitizer.core.config.SensitiveConfigLoader;
import com.desensitizer.core.config.SensitiveRule;
import com.desensitizer.core.handler.MapTypeHandler;
import com.desensitizer.core.handler.ObjectTypeHandler;
import com.desensitizer.core.handler.StringTypeHandler;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SensitiveLog {

    private static final class Holder {
        static final SensitiveLog INSTANCE = new SensitiveLog();
    }

    private final Map<SensitiveType, Desensitizer> desensitizerMap;
    private final Map<String, SensitiveType> fieldNameToType;
    private final ObjectTypeHandler objectTypeHandler;
    private final MapTypeHandler mapTypeHandler;
    private final StringTypeHandler stringTypeHandler;
    private final boolean enabled;

    private SensitiveLog() {
        this.desensitizerMap = new HashMap<>();
        initDesensitizers();

        SensitiveConfig config = loadConfigSafely();
        this.enabled = config != null && config.isEnabled();
        Map<String, SensitiveType> tempFieldMap = new HashMap<>();
        if (config != null) {
            initRules(config.getRules(), tempFieldMap);
        }
        this.fieldNameToType = Collections.unmodifiableMap(tempFieldMap);

        this.objectTypeHandler = new ObjectTypeHandler(desensitizerMap);
        this.mapTypeHandler = new MapTypeHandler(fieldNameToType, desensitizerMap);
        this.stringTypeHandler = new StringTypeHandler(desensitizerMap);
    }

    private SensitiveConfig loadConfigSafely() {
        try {
            return SensitiveConfigLoader.load();
        } catch (Exception e) {
            System.err.println("[SensitiveLog] 加载脱敏配置失败: " + e.getMessage());
            return null;
        }
    }

    public static SensitiveLog getInstance() {
        return Holder.INSTANCE;
    }

    private void initDesensitizers() {
        try {
            Class<?> phoneClass = Class.forName("com.desensitizer.builtin.desensitizer.PhoneDesensitizer");
            desensitizerMap.put(SensitiveType.PHONE, (Desensitizer) phoneClass.newInstance());
        } catch (Exception e) {
        }
        try {
            Class<?> idCardClass = Class.forName("com.desensitizer.builtin.desensitizer.IdCardDesensitizer");
            desensitizerMap.put(SensitiveType.ID_CARD, (Desensitizer) idCardClass.newInstance());
        } catch (Exception e) {
        }
        try {
            Class<?> nameClass = Class.forName("com.desensitizer.builtin.desensitizer.NameDesensitizer");
            desensitizerMap.put(SensitiveType.NAME, (Desensitizer) nameClass.newInstance());
        } catch (Exception e) {
        }
    }

    public void registerDesensitizer(SensitiveType type, Desensitizer desensitizer) {
        desensitizerMap.put(type, desensitizer);
    }

    private void initRules(List<SensitiveRule> rules, Map<String, SensitiveType> fieldMap) {
        if (rules == null) {
            return;
        }
        for (SensitiveRule rule : rules) {
            try {
                SensitiveType type = rule.getType();
                List<String> fieldNames = rule.getFieldNames();
                if (type == null || fieldNames == null) {
                    continue;
                }
                for (String fieldName : fieldNames) {
                    fieldMap.put(fieldName, type);
                }
            } catch (Exception e) {
                System.err.println("[SensitiveLog] 初始化规则失败: " + e.getMessage());
            }
        }
    }

    public static Object desensitize(Object obj) {
        try {
            if (!getInstance().enabled) {
                return obj;
            }
            return getInstance().objectTypeHandler.handle(obj);
        } catch (Exception e) {
            System.err.println("[SensitiveLog] 对象脱敏异常: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            return obj;
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> desensitize(Map<String, Object> map) {
        try {
            if (!getInstance().enabled) {
                return map;
            }
            return getInstance().mapTypeHandler.handle(map);
        } catch (Exception e) {
            System.err.println("[SensitiveLog] Map脱敏异常: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            return map;
        }
    }

    public static String desensitize(String content) {
        try {
            if (!getInstance().enabled) {
                return content;
            }
            return getInstance().stringTypeHandler.handle(content);
        } catch (Exception e) {
            System.err.println("[SensitiveLog] 字符串脱敏异常: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            return content;
        }
    }

    public static String desensitize(String content, SensitiveType type) {
        try {
            if (!getInstance().enabled) {
                return content;
            }
            return getInstance().stringTypeHandler.handle(content, type);
        } catch (Exception e) {
            System.err.println("[SensitiveLog] 字符串脱敏异常: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            return content;
        }
    }

    public static boolean isEnabled() {
        return getInstance().enabled;
    }

    public static void reset() {
        SensitiveConfigLoader.reset();
    }
}