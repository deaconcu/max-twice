package com.prosper.learn.api.application;

import cn.dev33.satoken.interceptor.SaInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import lombok.extern.java.Log;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.sql.DataSource;
import java.sql.Connection;

@Configuration
@Log
@MapperScan(basePackages = "com.prosper.learn.persistence.mapper")
@EnableScheduling
public class AppConfiguration implements WebMvcConfigurer {

    @Bean
    @ConfigurationProperties("spring.datasource")
    public DataSource dataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    public DataSourceTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    // 强制在应用启动时启动连接池
    @Bean
    public ApplicationRunner runner(DataSource dataSource) {
        return args -> dataSource.getConnection();
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 允许跨域的路径
                .allowedOrigins("http://localhost:5173", "http://localhost:5176") // 允许的源
                .allowCredentials(true)
                .allowedMethods("GET", "POST", "PUT", "DELETE") // 允许的请求方法
                .allowedHeaders("*"); // 允许的请求头
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory, ObjectMapper objectMapper) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        
        // 使用已配置的 ObjectMapper 创建序列化器
        ObjectMapper redisObjectMapper = objectMapper.copy();
        
        // 启用默认类型信息以保持类型安全
        redisObjectMapper.activateDefaultTyping(
                redisObjectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
        );
        
        org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer jsonRedisSerializer = 
                new org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer(redisObjectMapper);
        
        // 设置键值序列化器
        redisTemplate.setKeySerializer(new org.springframework.data.redis.serializer.StringRedisSerializer());
        redisTemplate.setValueSerializer(jsonRedisSerializer);
        redisTemplate.setHashKeySerializer(new org.springframework.data.redis.serializer.StringRedisSerializer());
        redisTemplate.setHashValueSerializer(jsonRedisSerializer);
        
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    @Bean
    public SaInterceptor saInterceptor() {
        return new SaInterceptor();  // 默认的 Sa-Token 拦截器
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor()).addPathPatterns("/**")
                .excludePathPatterns("/login");  // 排除不需要验证的路径
    }

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = JsonMapper.builder().findAndAddModules().build();

        // 禁用将日期写成时间戳
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
