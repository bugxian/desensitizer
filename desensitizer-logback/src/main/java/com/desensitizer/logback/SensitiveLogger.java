package com.desensitizer.logback;

import com.desensitizer.core.SensitiveLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

import java.util.Map;

public class SensitiveLogger implements Logger {

    private final Logger logger;

    public SensitiveLogger(Class<?> clazz) {
        this.logger = LoggerFactory.getLogger(clazz);
    }

    public SensitiveLogger(String name) {
        this.logger = LoggerFactory.getLogger(name);
    }

    @Override
    public String getName() {
        return logger.getName();
    }

    @Override
    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    @Override
    public void trace(String s) {
        logger.trace(s);
    }

    @Override
    public void trace(String s, Object o) {
        logger.trace(s, desensitizeArgument(o));
    }

    @Override
    public void trace(String s, Object o, Object o1) {
        logger.trace(s, desensitizeArgument(o), desensitizeArgument(o1));
    }

    @Override
    public void trace(String s, Object... objects) {
        logger.trace(s, desensitizeArguments(objects));
    }

    @Override
    public void trace(String s, Throwable throwable) {
        logger.trace(s, throwable);
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return logger.isTraceEnabled(marker);
    }

    @Override
    public void trace(Marker marker, String s) {
        logger.trace(marker, s);
    }

    @Override
    public void trace(Marker marker, String s, Object o) {
        logger.trace(marker, s, desensitizeArgument(o));
    }

    @Override
    public void trace(Marker marker, String s, Object o, Object o1) {
        logger.trace(marker, s, desensitizeArgument(o), desensitizeArgument(o1));
    }

    @Override
    public void trace(Marker marker, String s, Object... objects) {
        logger.trace(marker, s, desensitizeArguments(objects));
    }

    @Override
    public void trace(Marker marker, String s, Throwable throwable) {
        logger.trace(marker, s, throwable);
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public void debug(String s) {
        logger.debug(s);
    }

    @Override
    public void debug(String s, Object o) {
        logger.debug(s, desensitizeArgument(o));
    }

    @Override
    public void debug(String s, Object o, Object o1) {
        logger.debug(s, desensitizeArgument(o), desensitizeArgument(o1));
    }

    @Override
    public void debug(String s, Object... objects) {
        logger.debug(s, desensitizeArguments(objects));
    }

    @Override
    public void debug(String s, Throwable throwable) {
        logger.debug(s, throwable);
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return logger.isDebugEnabled(marker);
    }

    @Override
    public void debug(Marker marker, String s) {
        logger.debug(marker, s);
    }

    @Override
    public void debug(Marker marker, String s, Object o) {
        logger.debug(marker, s, desensitizeArgument(o));
    }

    @Override
    public void debug(Marker marker, String s, Object o, Object o1) {
        logger.debug(marker, s, desensitizeArgument(o), desensitizeArgument(o1));
    }

    @Override
    public void debug(Marker marker, String s, Object... objects) {
        logger.debug(marker, s, desensitizeArguments(objects));
    }

    @Override
    public void debug(Marker marker, String s, Throwable throwable) {
        logger.debug(marker, s, throwable);
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    @Override
    public void info(String s) {
        logger.info(s);
    }

    @Override
    public void info(String s, Object o) {
        logger.info(s, desensitizeArgument(o));
    }

    @Override
    public void info(String s, Object o, Object o1) {
        logger.info(s, desensitizeArgument(o), desensitizeArgument(o1));
    }

    @Override
    public void info(String s, Object... objects) {
        logger.info(s, desensitizeArguments(objects));
    }

    @Override
    public void info(String s, Throwable throwable) {
        logger.info(s, throwable);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return logger.isInfoEnabled(marker);
    }

    @Override
    public void info(Marker marker, String s) {
        logger.info(marker, s);
    }

    @Override
    public void info(Marker marker, String s, Object o) {
        logger.info(marker, s, desensitizeArgument(o));
    }

    @Override
    public void info(Marker marker, String s, Object o, Object o1) {
        logger.info(marker, s, desensitizeArgument(o), desensitizeArgument(o1));
    }

    @Override
    public void info(Marker marker, String s, Object... objects) {
        logger.info(marker, s, desensitizeArguments(objects));
    }

    @Override
    public void info(Marker marker, String s, Throwable throwable) {
        logger.info(marker, s, throwable);
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    @Override
    public void warn(String s) {
        logger.warn(s);
    }

    @Override
    public void warn(String s, Object o) {
        logger.warn(s, desensitizeArgument(o));
    }

    @Override
    public void warn(String s, Object o, Object o1) {
        logger.warn(s, desensitizeArgument(o), desensitizeArgument(o1));
    }

    @Override
    public void warn(String s, Object... objects) {
        logger.warn(s, desensitizeArguments(objects));
    }

    @Override
    public void warn(String s, Throwable throwable) {
        logger.warn(s, throwable);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return logger.isWarnEnabled(marker);
    }

    @Override
    public void warn(Marker marker, String s) {
        logger.warn(marker, s);
    }

    @Override
    public void warn(Marker marker, String s, Object o) {
        logger.warn(marker, s, desensitizeArgument(o));
    }

    @Override
    public void warn(Marker marker, String s, Object o, Object o1) {
        logger.warn(marker, s, desensitizeArgument(o), desensitizeArgument(o1));
    }

    @Override
    public void warn(Marker marker, String s, Object... objects) {
        logger.warn(marker, s, desensitizeArguments(objects));
    }

    @Override
    public void warn(Marker marker, String s, Throwable throwable) {
        logger.warn(marker, s, throwable);
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    @Override
    public void error(String s) {
        logger.error(s);
    }

    @Override
    public void error(String s, Object o) {
        logger.error(s, desensitizeArgument(o));
    }

    @Override
    public void error(String s, Object o, Object o1) {
        logger.error(s, desensitizeArgument(o), desensitizeArgument(o1));
    }

    @Override
    public void error(String s, Object... objects) {
        logger.error(s, desensitizeArguments(objects));
    }

    @Override
    public void error(String s, Throwable throwable) {
        logger.error(s, throwable);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return logger.isErrorEnabled(marker);
    }

    @Override
    public void error(Marker marker, String s) {
        logger.error(marker, s);
    }

    @Override
    public void error(Marker marker, String s, Object o) {
        logger.error(marker, s, desensitizeArgument(o));
    }

    @Override
    public void error(Marker marker, String s, Object o, Object o1) {
        logger.error(marker, s, desensitizeArgument(o), desensitizeArgument(o1));
    }

    @Override
    public void error(Marker marker, String s, Object... objects) {
        logger.error(marker, s, desensitizeArguments(objects));
    }

    @Override
    public void error(Marker marker, String s, Throwable throwable) {
        logger.error(marker, s, throwable);
    }

    private Object desensitizeArgument(Object arg) {
        if (arg == null) {
            return null;
        } else if (arg instanceof String) {
            return SensitiveLog.desensitize((String) arg);
        } else if (arg instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) arg;
            return SensitiveLog.desensitize(map);
        } else if (isBasicType(arg.getClass())) {
            return arg;
        } else {
            return SensitiveLog.desensitize(arg);
        }
    }

    private Object[] desensitizeArguments(Object... arguments) {
        if (arguments == null || arguments.length == 0) {
            return arguments;
        }
        Object[] result = new Object[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            result[i] = desensitizeArgument(arguments[i]);
        }
        return result;
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
                || clazz == Character.class
                || Number.class.isAssignableFrom(clazz);
    }
}