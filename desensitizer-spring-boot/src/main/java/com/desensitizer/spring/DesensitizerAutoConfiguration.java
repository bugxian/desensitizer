package com.desensitizer.spring;

import com.desensitizer.core.api.SensitiveType;
import com.desensitizer.core.detector.RegexDetector;
import com.desensitizer.core.engine.DesensitizationEngine;
import com.desensitizer.core.monitor.DesensitizationMonitor;
import com.desensitizer.core.registry.DesensitizerRegistry;
import com.desensitizer.builtin.desensitizer.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@AutoConfiguration
@EnableConfigurationProperties(DesensitizerProperties.class)
public class DesensitizerAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(DesensitizerAutoConfiguration.class);
    private final DesensitizerProperties properties;

    @Autowired
    private ApplicationContext applicationContext;

    public DesensitizerAutoConfiguration(DesensitizerProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnMissingBean
    public DesensitizerRegistry desensitizerRegistry() {
        DesensitizerRegistry registry = new DesensitizerRegistry();

        registry.register(SensitiveType.PHONE, new PhoneDesensitizer());
        registry.register(SensitiveType.ID_CARD, new IdCardDesensitizer());
        registry.register(SensitiveType.BANK_CARD, new BankCardDesensitizer());
        registry.register(SensitiveType.EMAIL, new EmailDesensitizer());
        registry.register(SensitiveType.PASSWORD, new PasswordDesensitizer());
        registry.register(SensitiveType.NAME, new NameDesensitizer());
        registry.register(SensitiveType.ADDRESS, new AddressDesensitizer());

        if (properties.getRegex().isEnabled()) {
            registry.registerDetector(SensitiveType.PHONE,
                    new RegexDetector("phone", SensitiveType.PHONE, properties.getRegex().getPhonePattern()));
            registry.registerDetector(SensitiveType.ID_CARD,
                    new RegexDetector("idCard", SensitiveType.ID_CARD, properties.getRegex().getIdCardPattern()));
            registry.registerDetector(SensitiveType.BANK_CARD,
                    new RegexDetector("bankCard", SensitiveType.BANK_CARD, properties.getRegex().getBankCardPattern()));
            registry.registerDetector(SensitiveType.EMAIL,
                    new RegexDetector("email", SensitiveType.EMAIL, properties.getRegex().getEmailPattern()));
            registry.registerDetector(SensitiveType.PASSWORD,
                    new RegexDetector("password", SensitiveType.PASSWORD, properties.getRegex().getPasswordPattern()));
            registry.registerDetector(SensitiveType.NAME,
                    new RegexDetector("name", SensitiveType.NAME, properties.getRegex().getNamePattern()));
            registry.registerDetector(SensitiveType.ADDRESS,
                    new RegexDetector("address", SensitiveType.ADDRESS, properties.getRegex().getAddressPattern()));
            registry.registerDetector(SensitiveType.ADDRESS,
                    new RegexDetector("addressKey", SensitiveType.ADDRESS, properties.getRegex().getAddressKeyPattern()));
        }

        return registry;
    }

    @Bean
    @ConditionalOnMissingBean
    public DesensitizationEngine desensitizationEngine(DesensitizerRegistry registry, DesensitizationMonitor monitor) {
        DesensitizationEngine engine = new DesensitizationEngine(registry);
        engine.setMonitor(monitor);
        return engine;
    }

    @Bean
    @ConditionalOnMissingBean
    public DesensitizationMonitor desensitizationMonitor() {
        return new DesensitizationMonitor();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void wrapLogbackAppenders(ApplicationReadyEvent event) {
        if (!properties.isEnabled() || !properties.getLog().isEnabled()) {
            logger.debug("Desensitization disabled");
            return;
        }

        try {
            Class.forName("ch.qos.logback.classic.Logger");
        } catch (ClassNotFoundException e) {
            logger.debug("Logback not found, skipping appender wrapping");
            return;
        }

        DesensitizationEngine engine = applicationContext.getBean(DesensitizationEngine.class);
        DesensitizationMonitor monitor = applicationContext.getBean(DesensitizationMonitor.class);

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger rootLogger = loggerContext.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);

        Map<String, Appender<ILoggingEvent>> appendersToReplace = new HashMap<>();
        
        Iterator<Appender<ILoggingEvent>> iterator = rootLogger.iteratorForAppenders();
        while (iterator.hasNext()) {
            Appender<ILoggingEvent> appender = iterator.next();
            if (shouldWrapAppender(appender)) {
                appendersToReplace.put(appender.getName(), appender);
            }
        }

        for (Map.Entry<String, Appender<ILoggingEvent>> entry : appendersToReplace.entrySet()) {
            Appender<ILoggingEvent> originalAppender = entry.getValue();
            String appenderName = entry.getKey();
            
            DesensitizingAppenderWrapper wrapper = new DesensitizingAppenderWrapper(
                    "Desensitizing-" + appenderName, originalAppender, engine, monitor);
            
            wrapper.setContext(loggerContext);
            wrapper.start();
            
            rootLogger.detachAppender(originalAppender);
            rootLogger.addAppender(wrapper);
            
            logger.info("Successfully wrapped appender: {} -> {}", appenderName, wrapper.getName());
        }

        if (appendersToReplace.isEmpty()) {
            logger.warn("No appenders found to wrap");
        }
    }

    private boolean shouldWrapAppender(Appender<ILoggingEvent> appender) {
        String appenderName = appender.getName();
        String appenderClass = appender.getClass().getSimpleName();

        if (appenderName == null || appenderName.startsWith("Desensitizing-")) {
            return false;
        }

        if (properties.getAppenders().getExclude().contains(appenderName) || 
            properties.getAppenders().getExclude().contains(appenderClass)) {
            return false;
        }

        if (!properties.getAppenders().getInclude().isEmpty()) {
            return properties.getAppenders().getInclude().contains(appenderName) || 
                   properties.getAppenders().getInclude().contains(appenderClass);
        }

        return true;
    }

    public static class DesensitizingAppenderWrapper extends ch.qos.logback.core.AppenderBase<ILoggingEvent> {
        private final Appender<ILoggingEvent> wrappedAppender;
        private final DesensitizationEngine engine;
        private final DesensitizationMonitor monitor;

        public DesensitizingAppenderWrapper(String name, Appender<ILoggingEvent> wrappedAppender, 
                                          DesensitizationEngine engine, DesensitizationMonitor monitor) {
            setName(name);
            this.wrappedAppender = wrappedAppender;
            this.engine = engine;
            this.monitor = monitor;
        }

        @Override
        public void start() {
            super.start();
            logger.debug("DesensitizingAppenderWrapper started: {}", getName());
        }

        @Override
        protected void append(ILoggingEvent event) {
            long startTime = System.nanoTime();
            try {
                String originalMessage = event.getMessage();
                String desensitizedMessage = engine.desensitize(originalMessage, false);  // 不在引擎内部记录，由外部统一记录
                
                long processingTime = System.nanoTime() - startTime;
                monitor.recordDesensitization("AUTO", processingTime);

                if (!originalMessage.equals(desensitizedMessage)) {
                    try {
                        java.lang.reflect.Field messageField = event.getClass().getDeclaredField("message");
                        messageField.setAccessible(true);
                        messageField.set(event, desensitizedMessage);
                    } catch (Exception e) {
                        logger.debug("Failed to replace message via reflection: {}", e.getMessage());
                    }
                }

                wrappedAppender.doAppend(event);
            } catch (Exception e) {
                monitor.recordError();
                logger.warn("Error during desensitization: {}", e.getMessage());
                try {
                    wrappedAppender.doAppend(event);
                } catch (Exception ex) {
                    logger.error("Failed to append original event", ex);
                }
            }
        }

        @Override
        public void stop() {
            super.stop();
            if (wrappedAppender != null && wrappedAppender.isStarted()) {
                wrappedAppender.stop();
            }
            logger.debug("DesensitizingAppenderWrapper stopped: {}", getName());
        }
    }
}
