package com.test.project1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TestApplication implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(TestApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }

    @Override
    public void run(String... args) {
        logger.info("测试手机号脱敏: 13812345678");
        logger.info("测试身份证脱敏: 110101199001011234");
        logger.info("测试银行卡脱敏: 6222021234567890123");
        logger.info("测试邮箱脱敏: test@example.com");
        logger.info("测试密码脱敏: password: mySecret123");
        logger.info("测试地址脱敏: 北京市朝阳区某某路123号");
    }
}
