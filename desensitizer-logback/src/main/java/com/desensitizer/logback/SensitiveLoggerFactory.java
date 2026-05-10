package com.desensitizer.logback;

import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SensitiveLoggerFactory {

    private static final Map<String, SensitiveLogger> loggerCache = new ConcurrentHashMap<>();
    private static final Map<Class<?>, SensitiveLogger> classLoggerCache = new ConcurrentHashMap<>();

    public static SensitiveLogger getLogger(String name) {
        return loggerCache.computeIfAbsent(name, SensitiveLogger::new);
    }

    public static SensitiveLogger getLogger(Class<?> clazz) {
        return classLoggerCache.computeIfAbsent(clazz, SensitiveLogger::new);
    }

    public static Logger getSLF4JLogger(String name) {
        return getLogger(name);
    }

    public static Logger getSLF4JLogger(Class<?> clazz) {
        return getLogger(clazz);
    }
}