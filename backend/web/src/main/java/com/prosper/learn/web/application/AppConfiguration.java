package com.prosper.learn.web.application;

import cn.dev33.satoken.interceptor.SaInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.prosper.learn.infrastructure.datasource.DataSourceInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;

@Configuration
@Log
@EnableScheduling
@RequiredArgsConstructor
public class AppConfiguration implements WebMvcConfigurer {

    private final DataSourceInterceptor dataSourceInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 允许跨域的路径
                //.allowedOrigins("http://localhost:5175", "http://localhost:5174") // 允许的源
                .allowedOriginPatterns("*") // 允许的源
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
        // 数据源路由拦截器（最先执行）
        registry.addInterceptor(dataSourceInterceptor)
                .addPathPatterns("/v1/**")
                .order(0);

        // Sa-Token 拦截器
        registry.addInterceptor(saInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns("/login", "/v1/public/**")
                .order(1);
    }

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = JsonMapper.builder().findAndAddModules().build();

        // 禁用将日期写成时间戳
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 设置日期格式和时区
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT+8"));
        mapper.setDateFormat(dateFormat);

        return mapper;
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(30))
                .readTimeout(Duration.ofSeconds(30))
                .build();
    }
}
