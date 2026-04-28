package com.twicemax.web.v2.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * API 日志切面
 * 用于记录所有 Controller 方法的调用信息，方便前后端联调
 *
 * 使用方式：
 * 在 application.yml 中设置日志级别为 INFO（默认就是INFO）：
 *   logging:
 *     level:
 *       aspect.v1.com.twicemax.web.ApiLoggingAspect: INFO
 *
 * 或者在开发环境启用 DEBUG 级别查看更详细的信息
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ApiLoggingAspect {

    private final ObjectMapper objectMapper;

    /**
     * 拦截所有 Controller 方法
     */
    @Around("@within(org.springframework.web.bind.annotation.RestController) || " +
            "@within(org.springframework.stereotype.Controller)")
    public Object logApi(ProceedingJoinPoint joinPoint) throws Throwable {
        // 只在 INFO 级别才记录
        if (!log.isInfoEnabled()) {
            return joinPoint.proceed();
        }

        long startTime = System.currentTimeMillis();

        // 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = attributes.getRequest();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 获取 HTTP 方法和路径
        String httpMethod = request.getMethod();
        String path = request.getRequestURI();

        // 获取 Controller 类名和方法名
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = method.getName();

        // 构建请求信息
        Map<String, Object> requestInfo = new HashMap<>();

        // 添加 URL 参数
        Map<String, String[]> paramMap = request.getParameterMap();
        if (!paramMap.isEmpty()) {
            Map<String, Object> params = new HashMap<>();
            paramMap.forEach((key, values) -> {
                if (values.length == 1) {
                    params.put(key, values[0]);
                } else {
                    params.put(key, values);
                }
            });
            requestInfo.put("params", params);
        }

        // 添加方法参数（@RequestBody, @PathVariable 等）
        Object[] args = joinPoint.getArgs();
        String[] paramNames = signature.getParameterNames();
        if (args != null && args.length > 0) {
            Map<String, Object> methodParams = new HashMap<>();
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                // 过滤掉 Servlet 相关的参数和其他不可序列化的对象
                if (arg != null && isSerializable(arg)) {
                    String paramName = paramNames != null && i < paramNames.length ?
                                      paramNames[i] : "arg" + i;
                    methodParams.put(paramName, arg);
                }
            }
            if (!methodParams.isEmpty()) {
                requestInfo.put("body", methodParams);
            }
        }

        try {
            // 打印请求日志
            String requestJson = requestInfo.isEmpty() ? "{}" :
                    safeJsonSerialize(requestInfo);

            log.info("\n" +
                    "╔════════════════════════════════════════════════════════════\n" +
                    "║ [API Request] {} {}\n" +
                    "║ Controller: {}.{}\n" +
                    "║ Request: {}\n" +
                    "╚════════════════════════════════════════════════════════════",
                    httpMethod, path, className, methodName, requestJson);

            // 执行方法
            Object result = joinPoint.proceed();

            // 计算耗时
            long duration = System.currentTimeMillis() - startTime;

            // 打印响应日志（格式化 JSON）
            String responseJson;
            if (result != null) {
                try {
                    // 格式化输出 JSON
                    responseJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
                    // 如果太长，截断
                    if (responseJson.length() > 2000) {
                        responseJson = responseJson.substring(0, 2000) + "\n... (truncated)";
                    }
                } catch (Exception e) {
                    responseJson = result.toString();
                }
            } else {
                responseJson = "null";
            }

            log.info("\n" +
                    "╔════════════════════════════════════════════════════════════\n" +
                    "║ [API Response] {} {}\n" +
                    "║ Response:\n{}\n" +
                    "║ Duration: {}ms\n" +
                    "╚════════════════════════════════════════════════════════════",
                    httpMethod, path, responseJson, duration);

            return result;

        } catch (Throwable throwable) {
            // 记录异常
            long duration = System.currentTimeMillis() - startTime;

            log.error("\n" +
                    "╔════════════════════════════════════════════════════════════\n" +
                    "║ [API Error] {} {}\n" +
                    "║ Controller: {}.{}\n" +
                    "║ Exception: {}\n" +
                    "║ Duration: {}ms\n" +
                    "╚════════════════════════════════════════════════════════════",
                    httpMethod, path, className, methodName,
                    throwable.getClass().getSimpleName() + ": " + throwable.getMessage(),
                    duration);

            throw throwable;
        }
    }

    /**
     * 判断对象是否可以安全序列化
     */
    private boolean isSerializable(Object obj) {
        if (obj == null) {
            return false;
        }

        String className = obj.getClass().getName();

        // 排除 Servlet 相关类
        if (className.startsWith("javax.servlet") ||
            className.startsWith("jakarta.servlet") ||
            className.startsWith("org.apache.catalina") ||
            className.startsWith("org.apache.tomcat")) {
            return false;
        }

        // 排除 Spring 框架类
        if (className.startsWith("org.springframework.web.context") ||
            className.startsWith("org.springframework.web.multipart") ||
            className.startsWith("org.springframework.validation")) {
            return false;
        }

        // 允许的类型
        return true;
    }

    /**
     * 安全地序列化对象为 JSON
     */
    private String safeJsonSerialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            // 序列化失败时，返回简单的字符串表示
            return obj.toString();
        }
    }
}
