package com.test.project1;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;

import java.util.logging.Logger;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ActiveProfiles("test")
public class DesensitizerTest {

    private static final Logger logger = Logger.getLogger(DesensitizerTest.class.getName());

    @Autowired
    private TestApplication testApplication;

    @Test
    public void testDesensitization() {
        // 测试手机号脱敏
        logger.info("Test phone: 13812345678");
        
        // 测试身份证号脱敏
        logger.info("Test ID card: 110101199001011234");
        
        // 测试银行卡号脱敏
        logger.info("Test bank card: 6222021234567890123");
        
        // 测试邮箱脱敏
        logger.info("Test email: user@example.com");
        
        // 测试密码脱敏
        logger.info("Test password: password123");
        
        // 测试地址脱敏
        logger.info("Test address: 北京市朝阳区建国路88号");
    }
}
