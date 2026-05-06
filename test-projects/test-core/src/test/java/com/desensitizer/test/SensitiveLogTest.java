package com.desensitizer.test;

import com.desensitizer.core.SensitiveLog;
import com.desensitizer.logback.SensitiveLoggerFactory;
import com.desensitizer.test.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class SensitiveLogTest {

    private static final Logger logger = SensitiveLoggerFactory.getSLF4JLogger(SensitiveLogTest.class);

    @BeforeEach
    public void setUp() {
        SensitiveLog.reset();
    }

    @Test
    public void testObjectType() {
        User user = new User("张三", "13800138000", "110101199001011234", "北京市");
        logger.info("对象类型脱敏结果 - 单参数: {}", user);
    }

    @Test
    public void testMapType() {
        Map<String, Object> map = new HashMap<>();
        map.put("phoneNo", "13800138000");
        map.put("sjh", "13900139000");
        map.put("phone", "13700137000");
        map.put("name", "李四");
        map.put("xm", "王五");
        map.put("address", "上海市");
        logger.info("Map类型脱敏结果 - 单参数: {}", map);
    }

    @Test
    public void testStringTypeAuto() {
        String content = "联系人: 张三, 电话: 13800138000, 身份证: 110101199001011234";
        logger.info("字符串自动探测脱敏结果 - 单参数: {}", content);
    }

    @Test
    public void testStringTypePlain() {
        logger.info("普通字符串 - 单参数: {}", "这是一条普通日志，不含敏感信息");
    }

    @Test
    public void testLogbackIntegration() {
        logger.info("多参数字符串测试 - 手机号: {}, 姓名: {}, 身份证: {}",
                "13800138000", "张三", "110101199001011234");
    }

    @Test
    public void testMultipleArgs() {
        User user = new User("张三", "13800138000", "110101199001011234", "北京市");

        Map<String, Object> map = new HashMap<>();
        map.put("phone", "13900139000");
        map.put("name", "李四");

        String str = "联系人: 王五, 电话: 13700137000";

        logger.info("多参数混合测试 - 对象: {}, Map: {}, 字符串: {}", user, map, str);
    }

    @Test
    public void testTwoArgs() {
        logger.info("双参数测试 - 姓名: {}, 手机号: {}", "赵六", "13600136000");
    }

    @Test
    public void testNoArgs() {
        logger.info("无参数测试 - 这是一条不含参数的日志");
    }

    @Test
    public void testThrowableArg() {
        Exception ex = new RuntimeException("测试异常: 13800138000");
        logger.error("异常测试 - 发生错误", ex);
    }

    @Test
    public void testDirectDesensitize() {
        String phone = SensitiveLog.desensitize("13800138000");
        System.out.println("直接脱敏手机号: " + phone);

        String idCard = SensitiveLog.desensitize("110101199001011234");
        System.out.println("直接脱敏身份证: " + idCard);

        String name = SensitiveLog.desensitize("联系人: 张三");
        System.out.println("直接脱敏姓名: " + name);
    }
}