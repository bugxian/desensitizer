package com.desensitizer.test;

import com.desensitizer.core.api.SensitiveType;
import com.desensitizer.core.engine.DesensitizationEngine;
import com.desensitizer.core.registry.DesensitizerRegistry;
import com.desensitizer.builtin.desensitizer.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class DesensitizerCoreTest {

    private final DesensitizationEngine engine;

    public DesensitizerCoreTest() {
        DesensitizerRegistry registry = new DesensitizerRegistry();

        registry.register(SensitiveType.PHONE, new PhoneDesensitizer());
        registry.register(SensitiveType.ID_CARD, new IdCardDesensitizer());
        registry.register(SensitiveType.BANK_CARD, new BankCardDesensitizer());
        registry.register(SensitiveType.EMAIL, new EmailDesensitizer());
        registry.register(SensitiveType.PASSWORD, new PasswordDesensitizer());
        registry.register(SensitiveType.ADDRESS, new AddressDesensitizer());

        registry.registerDetector(SensitiveType.PHONE, new com.desensitizer.core.detector.RegexDetector("phone", SensitiveType.PHONE, "1[3-9]\\d{9}"));
        registry.registerDetector(SensitiveType.ID_CARD, new com.desensitizer.core.detector.RegexDetector("idCard", SensitiveType.ID_CARD, "[1-9]\\d{5}(\\d{4})\\d{4}(\\d{3}[\\dXx])"));
        registry.registerDetector(SensitiveType.BANK_CARD, new com.desensitizer.core.detector.RegexDetector("bankCard", SensitiveType.BANK_CARD, "[621789]\\d{15,19}"));
        registry.registerDetector(SensitiveType.EMAIL, new com.desensitizer.core.detector.RegexDetector("email", SensitiveType.EMAIL, "\\w+@\\w+\\.\\w+"));
        registry.registerDetector(SensitiveType.PASSWORD, new com.desensitizer.core.detector.RegexDetector("password", SensitiveType.PASSWORD, "(?i)(password|pwd|密码)\\s*[:=]\\s*\\S+"));
        registry.registerDetector(SensitiveType.ADDRESS, new com.desensitizer.core.detector.RegexDetector("address", SensitiveType.ADDRESS, ".*[市县区].*[路街道].*"));

        this.engine = new DesensitizationEngine(registry);
    }

    @Test
    void testPhoneDesensitization() {
        String input = "用户手机号: 13812345678";
        String output = engine.desensitize(input);
        assertThat(output).contains("138****5678");
    }

    @Test
    void testIdCardDesensitization() {
        String input = "身份证号: 110101199001011234";
        String output = engine.desensitize(input);
        assertThat(output).contains("110101********1234");
    }

    @Test
    void testBankCardDesensitization() {
        String input = "银行卡号: 6222021234567890123";
        String output = engine.desensitize(input);
        assertThat(output).contains("622202********0123");
    }

    @Test
    void testEmailDesensitization() {
        String input = "邮箱: test@example.com";
        String output = engine.desensitize(input);
        assertThat(output).contains("t***@example.com");
    }

    @Test
    void testPasswordDesensitization() {
        String input = "密码: mySecretPassword";
        String output = engine.desensitize(input);
        assertThat(output).contains("****************");
    }

    @Test
    void testAddressDesensitization() {
        String input = "地址: 北京市朝阳区某某路123号";
        String output = engine.desensitize(input);
        assertThat(output).contains("北京市朝阳区***");
    }

    @Test
    void testMultipleSensitiveTypes() {
        String input = "用户手机号: 13812345678, 身份证: 110101199001011234, 银行卡: 6222021234567890123, 邮箱: test@example.com";
        String output = engine.desensitize(input);
        assertThat(output).contains("138****5678");
        assertThat(output).contains("110101********1234");
        assertThat(output).contains("622202********0123");
        assertThat(output).contains("t***@example.com");
    }

    @Test
    void testNoSensitiveInfo() {
        String input = "普通日志信息，无敏感数据";
        String output = engine.desensitize(input);
        assertThat(output).isEqualTo(input);
    }
}
