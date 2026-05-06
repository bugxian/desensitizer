package com.desensitizer.core.handler;

import com.desensitizer.core.api.Desensitizer;
import com.desensitizer.core.api.SensitiveType;

import java.util.HashMap;
import java.util.Map;

public class MapTypeHandler {

    private final Map<String, SensitiveType> fieldNameToType;
    private final Map<SensitiveType, Desensitizer> desensitizerMap;

    public MapTypeHandler(Map<String, SensitiveType> fieldNameToType,
                          Map<SensitiveType, Desensitizer> desensitizerMap) {
        this.fieldNameToType = fieldNameToType;
        this.desensitizerMap = desensitizerMap;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> handle(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            try {
                String key = entry.getKey();
                Object value = entry.getValue();
                SensitiveType type = fieldNameToType.get(key);
                if (type != null && value instanceof String) {
                    Desensitizer desensitizer = desensitizerMap.get(type);
                    if (desensitizer != null) {
                        result.put(key, desensitizer.desensitize((String) value));
                    } else {
                        result.put(key, value);
                    }
                } else if (value instanceof Map) {
                    result.put(key, handle((Map<String, Object>) value));
                } else {
                    result.put(key, value);
                }
            } catch (Exception e) {
                System.err.println("[MapTypeHandler] Map条目脱敏异常: key=" + entry.getKey()
                        + " - " + e.getClass().getSimpleName() + " - " + e.getMessage());
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }
}