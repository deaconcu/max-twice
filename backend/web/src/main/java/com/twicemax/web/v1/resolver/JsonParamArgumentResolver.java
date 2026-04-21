package com.twicemax.web.v1.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.twicemax.web.v1.annotation.JsonParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * JSON 参数解析器
 * 支持将 JSON 请求体中的字段直接映射到方法参数
 */
@Slf4j
@Component
public class JsonParamArgumentResolver implements HandlerMethodArgumentResolver {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String JSON_BODY_ATTRIBUTE = "JSON_REQUEST_BODY";
    
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(JsonParam.class);
    }
    
    @Override
    public Object resolveArgument(MethodParameter parameter,
                                ModelAndViewContainer mavContainer,
                                NativeWebRequest webRequest,
                                WebDataBinderFactory binderFactory) throws Exception {
        
        JsonParam annotation = parameter.getParameterAnnotation(JsonParam.class);
        String fieldName = annotation.value();
        boolean required = annotation.required();
        
        // 获取 JSON 请求体
        JsonNode jsonNode = getJsonBody(webRequest);
        if (jsonNode == null) {
            if (required) {
                throw new IllegalArgumentException("Required JSON request body is missing");
            }
            return null;
        }
        
        // 获取指定字段
        JsonNode fieldValue = jsonNode.get(fieldName);
        if (fieldValue == null || fieldValue.isNull()) {
            if (required) {
                throw new IllegalArgumentException("Required parameter '" + fieldName + "' is missing");
            }
            return null;
        }
        
        // 转换为目标类型
        return convertValue(fieldValue, parameter.getParameterType(), fieldName);
    }
    
    /**
     * 获取并缓存 JSON 请求体
     */
    private JsonNode getJsonBody(NativeWebRequest webRequest) throws IOException {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        
        // 检查是否已经解析过
        Object cached = request.getAttribute(JSON_BODY_ATTRIBUTE);
        if (cached != null) {
            return (JsonNode) cached;
        }
        
        // 读取请求体
        String body = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
        if (body.isEmpty()) {
            return null;
        }
        
        // 解析 JSON
        JsonNode jsonNode = objectMapper.readTree(body);
        
        // 缓存结果
        request.setAttribute(JSON_BODY_ATTRIBUTE, jsonNode);
        
        return jsonNode;
    }
    
    /**
     * 将 JsonNode 转换为目标类型
     */
    private Object convertValue(JsonNode fieldValue, Class<?> targetType, String fieldName) {
        try {
            if (targetType == String.class) {
                return fieldValue.asText();
            } else if (targetType == Integer.class || targetType == int.class) {
                return fieldValue.asInt();
            } else if (targetType == Long.class || targetType == long.class) {
                return fieldValue.asLong();
            } else if (targetType == Double.class || targetType == double.class) {
                return fieldValue.asDouble();
            } else if (targetType == Float.class || targetType == float.class) {
                return (float) fieldValue.asDouble();
            } else if (targetType == Boolean.class || targetType == boolean.class) {
                return fieldValue.asBoolean();
            } else if (targetType == BigDecimal.class) {
                return fieldValue.decimalValue();
            } else if (targetType == LocalDate.class) {
                return LocalDate.parse(fieldValue.asText());
            } else if (targetType == LocalDateTime.class) {
                return LocalDateTime.parse(fieldValue.asText(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } else {
                // 对于复杂类型，使用 ObjectMapper 转换
                return objectMapper.convertValue(fieldValue, targetType);
            }
        } catch (Exception e) {
            log.error("参数解析 字段 '{}' 转换为类型 {} 失败: {}", fieldName, targetType.getSimpleName(), e.getMessage());
            throw new IllegalArgumentException(
                String.format("Failed to convert field '%s' to %s: %s", 
                    fieldName, targetType.getSimpleName(), e.getMessage()), e);
        }
    }
}