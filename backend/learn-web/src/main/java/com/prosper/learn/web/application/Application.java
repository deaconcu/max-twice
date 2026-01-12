package com.prosper.learn.web.application;

import org.apache.ibatis.annotations.*;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = "com.prosper.learn")
@EnableDiscoveryClient
@EnableCaching
@EnableAsync  // 启用异步支持
@MapperScan(
    basePackages = "com.prosper.learn",
    annotationClass = Mapper.class
)
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
