package com.desensitizer.test;

import com.desensitizer.core.api.SensitiveType;
import com.desensitizer.core.detector.RegexDetector;
import com.desensitizer.core.engine.DesensitizationEngine;
import com.desensitizer.core.registry.DesensitizerRegistry;
import com.desensitizer.builtin.desensitizer.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 单条日志脱敏单元测试
 */
public class SingleLogDesensitizationTest {

    private DesensitizationEngine engine;

    @BeforeEach
    public void setUp() {
        DesensitizerRegistry registry = new DesensitizerRegistry();

        // 注册脱敏器
        registry.register(SensitiveType.PHONE, new PhoneDesensitizer());
        registry.register(SensitiveType.ID_CARD, new IdCardDesensitizer());
        registry.register(SensitiveType.BANK_CARD, new BankCardDesensitizer());
        registry.register(SensitiveType.EMAIL, new EmailDesensitizer());
        registry.register(SensitiveType.PASSWORD, new PasswordDesensitizer());
        registry.register(SensitiveType.ADDRESS, new AddressDesensitizer());
        registry.register(SensitiveType.NAME, new NameDesensitizer());

        // 注册检测器
        registry.registerDetector(SensitiveType.PHONE, new RegexDetector("phone", SensitiveType.PHONE, "1[3-9]\\d{9}"));
        registry.registerDetector(SensitiveType.ID_CARD, new RegexDetector("idCard", SensitiveType.ID_CARD, "[1-9]\\d{5}(\\d{4})\\d{4}(\\d{3}[\\dXx])"));
        registry.registerDetector(SensitiveType.BANK_CARD, new RegexDetector("bankCard", SensitiveType.BANK_CARD, "[621789]\\d{15,19}"));
        registry.registerDetector(SensitiveType.EMAIL, new RegexDetector("email", SensitiveType.EMAIL, "\\w+@\\w+\\.\\w+"));
        registry.registerDetector(SensitiveType.PASSWORD, new RegexDetector("password", SensitiveType.PASSWORD, "(?i)(password|pwd|密码)\\s*[:=]\\s*\\S+"));
        registry.registerDetector(SensitiveType.NAME, new RegexDetector("name", SensitiveType.NAME, "name=([^,\\s]+(?:\\s+[^,\\s]+){0,2})"));
        registry.registerDetector(SensitiveType.ADDRESS, new RegexDetector("address", SensitiveType.ADDRESS, "address=([^,]+)"));

        engine = new DesensitizationEngine(registry);
    }

    @Test
    public void testSingleLogDesensitization() {
        String originalLog = "Excel测试数据[1]: name=王桂珍,phone=15912342146,idCard=530427199001017727X,bankCard=6229081234567890884,address=青海省佛山县白云南昌路j座 705068";
        String desensitizedLog = engine.desensitize(originalLog);

        System.out.println("原始日志: " + originalLog);
        System.out.println("脱敏后: " + desensitizedLog);

        // 验证姓名脱敏
        assertTrue(desensitizedLog.contains("name=王*珍"), "姓名未正确脱敏");
        assertFalse(desensitizedLog.contains("王桂珍"), "姓名脱敏不完整");

        // 验证手机号脱敏
        assertTrue(desensitizedLog.contains("phone=159****2146"), "手机号未正确脱敏");
        assertFalse(desensitizedLog.contains("15912342146"), "手机号脱敏不完整");

        // 验证身份证号脱敏
        assertTrue(desensitizedLog.contains("idCard=530427199****7727X"), "身份证号未正确脱敏");
        assertFalse(desensitizedLog.contains("530427199001017727X"), "身份证号脱敏不完整");

        // 验证银行卡号脱敏
        assertTrue(desensitizedLog.contains("bankCard=622908********9884"), "银行卡号未正确脱敏");
        assertFalse(desensitizedLog.contains("6229081234567890884"), "银行卡号脱敏不完整");

        // 验证地址脱敏
        assertTrue(desensitizedLog.contains("address=青海省佛山县***"), "地址未正确脱敏");
        assertFalse(desensitizedLog.contains("白云南昌路j座"), "地址脱敏不完整");
    }

    @Test
    public void testChineseNameDesensitization() {
        String original = "name=张小明";
        String desensitized = engine.desensitize(original);
        assertEquals("name=张*明", desensitized, "三字姓名脱敏失败");
    }

    @Test
    public void testTwoCharNameDesensitization() {
        String original = "name=孔刘";
        String desensitized = engine.desensitize(original);
        assertEquals("name=孔*", desensitized, "两字姓名脱敏失败");
    }

    @Test
    public void testForeignNameDesensitization() {
        String original = "name=Felicitas Carsten";
        String desensitized = engine.desensitize(original);
        assertTrue(desensitized.contains("*"), "外文姓名未脱敏");
        assertFalse(desensitized.contains("Felicitas Carsten"), "外文姓名脱敏不完整");
    }

    @Test
    public void testAddressDesensitization() {
        String original = "address=北京市朝阳区建国路88号";
        String desensitized = engine.desensitize(original);
        assertTrue(desensitized.contains("北京市朝阳区"), "地址省市级未保留");
        assertTrue(desensitized.contains("***"), "地址详情未脱敏");
    }

    @Test
    public void testMixedLogDesensitization() {
        String original = "用户登录: name=张三,phone=13812345678,email=test@example.com,password=123456";
        String desensitized = engine.desensitize(original);

        System.out.println("原始: " + original);
        System.out.println("脱敏后: " + desensitized);

        assertTrue(desensitized.contains("张*"), "姓名未脱敏");
        assertTrue(desensitized.contains("138****5678"), "手机号未脱敏");
        assertTrue(desensitized.contains("test@example.com"), "邮箱应保持原样");
        assertTrue(desensitized.contains("******"), "密码未脱敏");
    }
}