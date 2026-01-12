package com.prosper.learn.shared.mybatis;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MyBatis 拦截器：自动填充 createdAt 和 updatedAt 字段
 */
@Slf4j
@Component
@Intercepts({
    @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})
})
public class TimestampInterceptor implements Interceptor {

    /**
     * 字段缓存：Class -> (FieldName -> Field)
     * 避免每次都通过反射查找字段，提升性能
     */
    private final Map<Class<?>, Map<String, Field>> fieldCache = new ConcurrentHashMap<>();

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
        SqlCommandType sqlCommandType = mappedStatement.getSqlCommandType();
        Object parameter = invocation.getArgs()[1];

        if (parameter == null) {
            return invocation.proceed();
        }

        LocalDateTime now = LocalDateTime.now();

        if (SqlCommandType.INSERT.equals(sqlCommandType)) {
            // INSERT 操作：设置 createdAt 和 updatedAt
            setFieldValue(parameter, "createdAt", now);
            setFieldValue(parameter, "updatedAt", now);
            log.debug("Auto-fill createdAt and updatedAt for INSERT: {}", parameter.getClass().getSimpleName());
        } else if (SqlCommandType.UPDATE.equals(sqlCommandType)) {
            // UPDATE 操作：设置 updatedAt
            setFieldValue(parameter, "updatedAt", now);
            log.debug("Auto-fill updatedAt for UPDATE: {}", parameter.getClass().getSimpleName());
        }

        return invocation.proceed();
    }

    /**
     * 使用反射设置字段值（带字段缓存优化）
     *
     * 使用 ConcurrentHashMap 缓存 Field 对象，避免每次都通过反射查找
     * 性能提升：从 O(n) 反射查找 → O(1) Map 查找
     */
    private void setFieldValue(Object obj, String fieldName, Object value) {
        try {
            // 从缓存获取或查找 Field
            Field field = fieldCache
                    .computeIfAbsent(obj.getClass(), k -> new ConcurrentHashMap<>())
                    .computeIfAbsent(fieldName, k -> findField(obj.getClass(), k));

            if (field != null) {
                field.setAccessible(true);
                // 只在字段为 null 时设置值
                if (field.get(obj) == null) {
                    field.set(obj, value);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to set field {} for {}: {}", fieldName, obj.getClass().getSimpleName(), e.getMessage());
        }
    }

    /**
     * 递归查找字段（包括父类）
     */
    private Field findField(Class<?> clazz, String fieldName) {
        if (clazz == null || clazz == Object.class) {
            return null;
        }
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            return findField(clazz.getSuperclass(), fieldName);
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // 无需配置属性
    }
}
