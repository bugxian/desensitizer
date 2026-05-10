package com.desensitizer.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AppenderBase;
import com.desensitizer.core.api.SensitiveType;
import com.desensitizer.core.detector.RegexDetector;
import com.desensitizer.core.engine.DesensitizationEngine;
import com.desensitizer.core.registry.DesensitizerRegistry;
import com.desensitizer.builtin.desensitizer.*;

import java.lang.reflect.Field;

public class DesensitizingLogbackAppender extends AppenderBase<ILoggingEvent> {

    private boolean enabled = true;
    private Appender<ILoggingEvent> wrappedAppender;
    private DesensitizationEngine engine;

    public DesensitizingLogbackAppender() {
        DesensitizerRegistry registry = new DesensitizerRegistry();

        registry.register(SensitiveType.PHONE, new PhoneDesensitizer());
        registry.register(SensitiveType.ID_CARD, new IdCardDesensitizer());
        registry.register(SensitiveType.BANK_CARD, new BankCardDesensitizer());
        registry.register(SensitiveType.EMAIL, new EmailDesensitizer());
        registry.register(SensitiveType.PASSWORD, new PasswordDesensitizer());
        registry.register(SensitiveType.ADDRESS, new AddressDesensitizer());
        registry.register(SensitiveType.NAME, new NameDesensitizer());

        registry.registerDetector(SensitiveType.PHONE, new RegexDetector("phone", SensitiveType.PHONE, "1[3-9]\\d{9}"));
        registry.registerDetector(SensitiveType.ID_CARD, new RegexDetector("idCard", SensitiveType.ID_CARD, "[1-9]\\d{5}\\d{4}\\d{4}\\d{3}[\\dXx]"));
        registry.registerDetector(SensitiveType.BANK_CARD, new RegexDetector("bankCard", SensitiveType.BANK_CARD, "[621789]\\d{15,19}"));
        registry.registerDetector(SensitiveType.EMAIL, new RegexDetector("email", SensitiveType.EMAIL, "\\w+@\\w+\\.\\w+"));
        registry.registerDetector(SensitiveType.PASSWORD, new RegexDetector("password", SensitiveType.PASSWORD, "(?i)(password|pwd|密码)\\s*[:=]\\s*\\S+"));
        registry.registerDetector(SensitiveType.NAME, new RegexDetector("name", SensitiveType.NAME, "姓名[：:]([^，,\\s]+(?:\\s+[^，,\\s]+){0,2})"));
        registry.registerDetector(SensitiveType.ADDRESS, new RegexDetector("address", SensitiveType.ADDRESS, "([\\u4e00-\\u9fa5]{2,}(?:省|市|县|区|镇|乡|村|街|路|道|巷|座|栋|单元|室)[\\u4e00-\\u9fa5a-zA-Z0-9\\s-]*[\\d座栋号室]?)|(\\d+\\s+[A-Za-z]+(?:\\s+[A-Za-z]+)*(?:,\\s*[A-Za-z\\s]+)*\\s*\\d{4,6})"));
        registry.registerDetector(SensitiveType.ADDRESS, new RegexDetector("addressKey", SensitiveType.ADDRESS, "地址[：:]([^，,]+)"));

        this.engine = new DesensitizationEngine(registry);
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (!enabled || wrappedAppender == null) {
            return;
        }

        String message = event.getMessage();
        String desensitizedMessage = engine.desensitize(message);

        // 尝试通过反射替换消息内容
        try {
            Field messageField = event.getClass().getDeclaredField("message");
            messageField.setAccessible(true);
            messageField.set(event, desensitizedMessage);
        } catch (Exception e) {
            // 反射失败时，直接使用原始事件
        }

        wrappedAppender.doAppend(event);
    }

    public void addAppender(Appender<ILoggingEvent> appender) {
        this.wrappedAppender = appender;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public DesensitizationEngine getEngine() {
        return engine;
    }
}
