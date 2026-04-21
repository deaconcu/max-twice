package com.twicemax.infrastructure.datasource;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * 业务模块 Mapper 扫描配置
 * 使用 businessSqlSessionFactory，连接到业务库（动态路由）
 */
@Configuration
@MapperScan(
    basePackages = {
        "com.twicemax.content",       // 内容模块
        "com.twicemax.interaction",   // 互动模块
        "com.twicemax.learning",      // 学习进度模块
        "com.twicemax.memory",        // 记忆卡片模块
        "com.twicemax.analytics",     // 统计分析模块
        "com.twicemax.infrastructure.image",  // 图片上传
        "com.twicemax.shared.infrastructure.config"  // 系统配置
    },
    sqlSessionFactoryRef = "businessSqlSessionFactory"
)
public class BusinessMapperConfig {
}
