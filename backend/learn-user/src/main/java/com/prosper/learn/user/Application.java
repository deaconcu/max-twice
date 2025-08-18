package com.prosper.learn.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.prosper.learn")
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
