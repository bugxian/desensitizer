package com.desensitizer.core.handler;

import com.desensitizer.core.annotation.Sensitive;
import com.desensitizer.core.api.Desensitizer;
import com.desensitizer.core.api.SensitiveType;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ObjectTypeHandler {

    private final Map<SensitiveType, Desensitizer> desensitizerMap;
    private final Map<Class<?>, Field[]> fieldCache = new ConcurrentHashMap<>();

    public ObjectTypeHandler(Map<SensitiveType, Desensitizer> desensitizerMap) {
        this.desensitizerMap = desensitizerMap;
    }

    public Object handle(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof String) {
            return obj;
        }
        if (obj instanceof Map) {
            return obj;
        }
        Class<?> clazz = obj.getClass();
        if (isBasicType(clazz)) {
            return obj;
        }

        try {
            String strValue = obj.toString();
            if (strValue == null || strValue.isEmpty() || strValue.startsWith(clazz.getName() + "@")) {
                return obj;
            }

            Field[] fields = getFields(clazz);
            String result = strValue;
            for (Field field : fields) {
                try {
                    Sensitive sensitive = field.getAnnotation(Sensitive.class);
                    if (sensitive == null) {
                        continue;
                    }
                    field.setAccessible(true);
                    Object value = field.get(obj);
                    if (value == null) {
                        continue;
                    }
                    SensitiveType type = sensitive.type();
                    if (value instanceof String) {
                        Desensitizer desensitizer = desensitizerMap.get(type);
                        if (desensitizer != null) {
                            String original = (String) value;
                            String masked = desensitizer.desensitize(original);
                            if (!original.equals(masked)) {
                                result = result.replace(original, masked);
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[ObjectTypeHandler] 字段脱敏异常: " + clazz.getName() + "." + field.getName()
                            + " - " + e.getClass().getSimpleName() + " - " + e.getMessage());
                }
            }
            return result;
        } catch (Exception e) {
            System.err.println("[ObjectTypeHandler] 对象脱敏异常: " + clazz.getName()
                    + " - " + e.getClass().getSimpleName() + " - " + e.getMessage());
            return obj;
        }
    }

    private Field[] getFields(Class<?> clazz) {
        return fieldCache.computeIfAbsent(clazz, k -> {
            Field[] fields = k.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
            }
            return fields;
        });
    }

    private boolean isBasicType(Class<?> clazz) {
        return clazz.isPrimitive()
                || clazz == Integer.class
                || clazz == Long.class
                || clazz == Double.class
                || clazz == Float.class
                || clazz == Boolean.class
                || clazz == Byte.class
                || clazz == Short.class
                || clazz == Character.class;
    }
}