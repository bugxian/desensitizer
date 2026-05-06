package com.desensitizer.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.desensitizer.spring", "com.desensitizer.test"})
public class TestConsoleApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestConsoleApplication.class, args);
    }
}
