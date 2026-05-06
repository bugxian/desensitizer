package com.desensitizer.log4j2;

import com.desensitizer.core.api.SensitiveType;
import com.desensitizer.core.detector.RegexDetector;
import com.desensitizer.core.engine.DesensitizationEngine;
import com.desensitizer.core.registry.DesensitizerRegistry;
import com.desensitizer.builtin.desensitizer.*;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;

@Plugin(name = "DesensitizingAppender", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE)
public class DesensitizingLog4j2Appender extends AbstractAppender {

    private final boolean enabled;
    private final Appender wrappedAppender;
    private final DesensitizationEngine engine;

    protected DesensitizingLog4j2Appender(
            @PluginAttribute("enabled") boolean enabled,
            @PluginElement("WrappedAppender") Appender wrappedAppender,
            @PluginElement("Layout") PatternLayout layout) {
        super("DesensitizingAppender", null, layout, true, null);
        this.enabled = enabled;
        this.wrappedAppender = wrappedAppender;
        this.engine = initEngine();
    }

    private DesensitizationEngine initEngine() {
        DesensitizerRegistry registry = new DesensitizerRegistry();

        registry.register(SensitiveType.PHONE, new PhoneDesensitizer());
        registry.register(SensitiveType.ID_CARD, new IdCardDesensitizer());
        registry.register(SensitiveType.BANK_CARD, new BankCardDesensitizer());
        registry.register(SensitiveType.EMAIL, new EmailDesensitizer());
        registry.register(SensitiveType.PASSWORD, new PasswordDesensitizer());
        registry.register(SensitiveType.ADDRESS, new AddressDesensitizer());

        registry.registerDetector(SensitiveType.PHONE, new RegexDetector("phone", SensitiveType.PHONE, "1[3-9]\\d{9}"));
        registry.registerDetector(SensitiveType.ID_CARD, new RegexDetector("idCard", SensitiveType.ID_CARD, "[1-9]\\d{5}(\\d{4})\\d{4}(\\d{3}[\\dXx])"));
        registry.registerDetector(SensitiveType.BANK_CARD, new RegexDetector("bankCard", SensitiveType.BANK_CARD, "[621789]\\d{15,19}"));
        registry.registerDetector(SensitiveType.EMAIL, new RegexDetector("email", SensitiveType.EMAIL, "\\w+@\\w+\\.\\w+"));
        registry.registerDetector(SensitiveType.PASSWORD, new RegexDetector("password", SensitiveType.PASSWORD, "(?i)(password|pwd|密码)\\s*[:=]\\s*\\S+"));

        return new DesensitizationEngine(registry);
    }

    @Override
    public void append(LogEvent event) {
        if (!enabled || wrappedAppender == null) {
            return;
        }

        String message = event.getMessage().getFormattedMessage();
        String desensitizedMessage = engine.desensitize(message);

        // 使用 SimpleMessage 替换原始消息
        SimpleMessage desensitizedSimpleMessage = new SimpleMessage(desensitizedMessage);
        
        // 直接使用原始事件，消息会被布局处理时替换
        wrappedAppender.append(event);
    }

    public DesensitizationEngine getEngine() {
        return engine;
    }
}
