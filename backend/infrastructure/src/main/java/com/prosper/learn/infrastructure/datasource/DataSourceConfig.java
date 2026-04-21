package com.prosper.learn.infrastructure.datasource;

import com.prosper.learn.shared.mybatis.TimestampInterceptor;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * 多数据源配置
 *
 * 数据源分为两类：
 * 1. userDataSource - 用户数据库（共享，不分语言）
 * 2. businessDataSource - 业务数据库（动态，按语言分）
 */
@Configuration
public class DataSourceConfig {

    // ==================== 原始数据源 ====================

    /**
     * 用户数据源（共享库）
     */
    @Bean(name = "userDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.user")
    public DataSource userDataSource() {
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    /**
     * 业务数据源 - 中文站
     */
    @Bean(name = "businessZhDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.business-zh")
    public DataSource businessZhDataSource() {
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    /**
     * 业务数据源 - 英文站
     */
    @Bean(name = "businessEnDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.business-en")
    public DataSource businessEnDataSource() {
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    // ==================== 动态数据源 ====================

    /**
     * 业务动态数据源（根据语言路由）
     */
    @Bean(name = "businessDataSource")
    @Primary
    public DataSource businessDataSource(
            @Qualifier("businessZhDataSource") DataSource businessZhDataSource,
            @Qualifier("businessEnDataSource") DataSource businessEnDataSource) {

        DynamicDataSource dynamicDataSource = new DynamicDataSource();

        // 设置目标数据源映射
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put("zh", businessZhDataSource);
        targetDataSources.put("en", businessEnDataSource);
        dynamicDataSource.setTargetDataSources(targetDataSources);

        // 设置默认数据源
        dynamicDataSource.setDefaultTargetDataSource(businessZhDataSource);

        return dynamicDataSource;
    }

    // ==================== SqlSessionFactory ====================

    /**
     * 用户模块 SqlSessionFactory
     */
    @Bean(name = "userSqlSessionFactory")
    public SqlSessionFactory userSqlSessionFactory(
            @Qualifier("userDataSource") DataSource dataSource,
            TimestampInterceptor timestampInterceptor) throws Exception {
        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(dataSource);

        // 配置 MyBatis
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        factory.setConfiguration(configuration);

        // 注册 MyBatis 拦截器（自动填充 createdAt / updatedAt）
        factory.setPlugins(timestampInterceptor);

        return factory.getObject();
    }

    /**
     * 业务模块 SqlSessionFactory
     */
    @Bean(name = "businessSqlSessionFactory")
    @Primary
    public SqlSessionFactory businessSqlSessionFactory(
            @Qualifier("businessDataSource") DataSource dataSource,
            TimestampInterceptor timestampInterceptor) throws Exception {
        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(dataSource);

        // 配置 MyBatis
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        factory.setConfiguration(configuration);

        // 注册 MyBatis 拦截器（自动填充 createdAt / updatedAt）
        factory.setPlugins(timestampInterceptor);

        return factory.getObject();
    }

    // ==================== SqlSessionTemplate ====================

    @Bean(name = "userSqlSessionTemplate")
    public SqlSessionTemplate userSqlSessionTemplate(
            @Qualifier("userSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    @Bean(name = "businessSqlSessionTemplate")
    @Primary
    public SqlSessionTemplate businessSqlSessionTemplate(
            @Qualifier("businessSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    // ==================== 事务管理器 ====================

    @Bean(name = "userTransactionManager")
    public PlatformTransactionManager userTransactionManager(
            @Qualifier("userDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean(name = "businessTransactionManager")
    @Primary
    public PlatformTransactionManager businessTransactionManager(
            @Qualifier("businessDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
