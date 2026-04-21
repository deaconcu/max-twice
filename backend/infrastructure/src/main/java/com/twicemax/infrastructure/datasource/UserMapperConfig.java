package com.twicemax.infrastructure.datasource;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * 用户模块 Mapper 扫描配置
 * 使用 userSqlSessionFactory，连接到用户共享库
 */
@Configuration
@MapperScan(
    basePackages = {
        "com.twicemax.user"  // 用户模块
    },
    sqlSessionFactoryRef = "userSqlSessionFactory"
)
public class UserMapperConfig {
}
