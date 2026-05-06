package com.test.project3;

import com.desensitizer.core.api.SensitiveType;
import com.desensitizer.core.detector.RegexDetector;
import com.desensitizer.core.engine.DesensitizationEngine;
import com.desensitizer.core.registry.DesensitizerRegistry;
import com.desensitizer.builtin.desensitizer.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestApplication {

    private static final Logger logger = LoggerFactory.getLogger(TestApplication.class);

    public static void main(String[] args) {
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

        DesensitizationEngine engine = new DesensitizationEngine(registry);

        logger.info(engine.desensitize("测试手机号脱敏: 13812345678"));
        logger.info(engine.desensitize("测试身份证脱敏: 110101199001011234"));
        logger.info(engine.desensitize("测试银行卡脱敏: 6222021234567890123"));
        logger.info(engine.desensitize("测试邮箱脱敏: test@example.com"));
        logger.info(engine.desensitize("测试密码脱敏: password: mySecret123"));
        logger.info(engine.desensitize("测试地址脱敏: 北京市朝阳区某某路123号"));
    }
}
