package com.prosper.learn.persistence.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Properties;

/**
 * MyBatis 拦截器：自动填充 createdAt 和 updatedAt 字段
 */
@Slf4j
@Component
@Intercepts({
    @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})
})
public class TimestampInterceptor implements Interceptor {

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
     * 使用反射设置字段值
     */
    private void setFieldValue(Object obj, String fieldName, Object value) {
        try {
            Field field = findField(obj.getClass(), fieldName);
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
