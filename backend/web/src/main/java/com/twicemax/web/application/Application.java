package com.twicemax.web.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = "com.twicemax")
@EnableCaching
@EnableAsync  // 启用异步支持
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
